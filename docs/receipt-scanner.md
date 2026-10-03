# Receipt scanner setup

Spendr sends receipt images to [vision-api](https://github.com/uzuki-P/vision-api).
The API runs an authenticated OpenCode or Codex CLI with an image-capable model.
Spendr uses asynchronous jobs and lets you review the merchant, total, and items
before saving a receipt. Manual entry works without this service.

## Prepare the service

Follow the upstream [setup guide](https://github.com/uzuki-P/vision-api/blob/main/docs/setup.md)
to install Bun and the service dependencies, install a compatible CLI, and
authenticate the selected model provider under the account that runs the API.
Create the private `.env` using the upstream instructions. Set `API_TOKEN` to a
random secret of at least 32 characters and choose `DEFAULT_PROVIDER` as
`opencode` or `codex`. Optional `DEFAULT_MODEL` and `DEFAULT_REASONING_EFFORT`
control requests that omit those fields. Start the service using its `just api`
recipe or its documented systemd service.

The API token authenticates Spendr to vision-api. The CLI's provider credentials
authenticate vision-api to the model provider. Enter only the API token in
Spendr. Keep both kinds of credentials out of Git.

## Make the API reachable

The upstream service binds to loopback. Put an HTTPS reverse proxy or private
HTTPS route in front of the API port. The proxy must forward `/v1/*`, bearer
authorization headers, and multipart uploads to the API. Point Spendr at the
API route, rather than the playground's web port or `/api` proxy.

Spendr requires an HTTPS base address with a hostname and an Android-trusted
TLS certificate. It rejects HTTP URLs, credentials in the URL, query strings,
and fragments. Enter the base address without `/v1/jobs`. The client appends
endpoint paths. `localhost` on the phone refers to the phone, not the server.

The app's preset address is `https://vision-api.ts.uzuki-p.my.id`. This address
does not grant access to the service. If using its private Tailscale route, the
phone must connect to the permitted tailnet and have access to that host.
For your own deployment, replace the address with your reachable HTTPS route.
Native Android requests do not need browser CORS configuration.

Allow receipt uploads up to 12 MiB through the proxy, plus multipart overhead.
The app accepts JPEG, PNG, WebP, and GIF. Individual HTTP requests use a
5-second connection timeout and a 15-second read timeout. Model processing
happens in the job after submission, so the upload response must return promptly.

## Configure Spendr

1. Open Settings → Receipt scanner.
2. Enter the HTTPS API address and its API token, without the `Bearer ` prefix.
3. Leave provider, model, and reasoning effort empty to use service defaults,
   or choose values supported by the service. Provider is `opencode` or `codex`.
4. Tap Test connection. A successful authenticated `/v1/providers` response
   enables image scanning. If model discovery succeeds, you can choose a model
   from the returned list. A connection test does not run an image request.
5. Tap Save settings, then open Home → Add receipt and choose an image or camera.
   Review the completed result before saving.

WorkManager polls about every three seconds in short batches and retries between
batches. Android may delay work when the app is in the background. The app stops
retrying after roughly 31 minutes. Allow notifications to receive completion
alerts. Unsaved results also appear in Add receipt. Stopping checks in Spendr
does not cancel the server job.

## Required API contract

All endpoints below require `Authorization: Bearer <API_TOKEN>` and return JSON.
The examples use sample values rather than credentials or real receipt data.
See the upstream [API documentation](https://github.com/uzuki-P/vision-api#api)
for server limits and additional response fields.

### Connection and model discovery

`GET /v1/providers` must return HTTP 200 with `default_provider`. Optional model
and effort defaults may be strings or null.

```json
{
  "providers": ["opencode", "codex"],
  "default_provider": "codex",
  "default_model": null,
  "default_reasoning_effort": null
}
```

`GET /v1/models?provider=codex` must return HTTP 200 with a `models` array.
Each model needs `id`; `label` and `reasoning_efforts` are optional. The same
endpoint accepts `provider=opencode`. Discovery failure leaves manual model
entry available.

```json
{
  "provider": "codex",
  "models": [
    {"id": "your-model-id", "label": "Your model", "reasoning_efforts": ["low", "high"]}
  ]
}
```

### Submit an image

`POST /v1/jobs` accepts `multipart/form-data` with `image` and `instruction`.
Spendr supplies an instruction to extract `merchant`, `total`, and `items`.
It includes `provider`, `model`, and `reasoning_effort` only when configured.
The image part includes its MIME type.

The server must return HTTP 202 with a UUID `id`. Spendr builds the poll URL
from this ID. Upstream also supplies `Location` and `Retry-After` headers,
but Spendr does not read them.

```json
{"id": "123e4567-e89b-12d3-a456-426614174000", "status": "queued"}
```

### Poll and return a receipt

`GET /v1/jobs/{id}` must return HTTP 200 with `status` set to `queued`, `running`,
`succeeded`, or `failed`. Queued and running responses need no result yet.
A successful response must nest the receipt inside `response.result`.

```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "status": "succeeded",
  "response": {
    "result": {
      "merchant": "Sample market",
      "total": 35000,
      "items": [
        {"name": "Rice", "paidAmount": 25000, "quantity": 1},
        {"name": "Fruit", "paidAmount": 10000, "quantity": 0.5}
      ]
    }
  }
}
```

`merchant` is a string or null. `total` is an integer amount in rupiah or null.
Spendr treats nonpositive totals as absent. `items` is an array of objects with
a nonblank `name`, a nonnegative integer `paidAmount` in rupiah, and a positive
`quantity`, including fractions. `paidAmount` is the whole line's paid amount,
not the unit price. Invalid names or negative amounts cause the client to skip
an item. Missing or invalid quantities default to 1. Subtotal, tax, discount,
change, payment, and total lines should not become items.

Upstream adds `_metadata` inside `response`; Spendr does not require it.
An implementation exposing only the synchronous `/v1/analyze` endpoint is
insufficient for this client.

Failed jobs use an error object:

```json
{"status": "failed", "error": {"code": "provider_failed", "message": "The scan failed."}}
```

HTTP errors use the same `error.code` and `error.message` shape. Spendr displays
the message when supplied. Missing or expired jobs return HTTP 404.

## Troubleshooting

| Symptom | Check |
| --- | --- |
| Connection fails | Phone network access, private route permissions, DNS, TLS certificate, and API port behind the proxy. |
| HTTP 401 or 403 | Match the service's API token and route access policy. Restart the API after token rotation. |
| Connection succeeds but scans fail | CLI login, image support, model access, reasoning effort, and model allowlist. |
| HTTP 413 or 415 | Image size, MIME type, and proxy upload limits. |
| HTTP 429 | Server rate or queue limits. Background work retries while within its time limit. |
| HTTP 404 while polling | Job expired or disappeared. Submit a new scan. |
| Result stays pending | Service availability, Android background scheduling, and server job timeout. |

## Token handling

Both flavors start with an empty token. Enter the API token manually in
Settings → Receipt scanner and save the settings. The app stores it in private
DataStore preferences and sends it as a bearer authorization header. Builds
do not read the service's `.env` or embed scanner tokens in the APK.

Main and dev keep separate Android data. Updating the APK preserves manually
saved scanner settings. Saved scan configurations also retain the token used
when the scan started. Password masking in Settings does not encrypt those
stored values.
