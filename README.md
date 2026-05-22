# URL-Shortener-Service

## CI/CD deployment

Pushes to the `develop` branch trigger `.github/workflows/deploy.yml`.

Required GitHub repository secrets:

- `EC2_HOST`: EC2 public IP or DNS name.
- `EC2_USERNAME`: SSH user for the EC2 instance, for example `ubuntu` or `ec2-user`.
- `EC2_SSH_KEY`: Private SSH key with access to the EC2 instance.

Add them in GitHub under `Settings` -> `Secrets and variables` -> `Actions` -> `New repository secret`.

The workflow builds the Spring Boot app, connects to EC2 over SSH, pulls the latest `develop` branch, and redeploys with Docker Compose:

```bash
git pull origin develop
sudo docker compose down
sudo docker compose up -d --build
```

The EC2 checkout is expected at `~/URL-Shortener-Service`.

## Swagger / OpenAPI

Interactive API documentation is available at:

https://url-shortener-service.duckdns.org/swagger-ui/index.html

The OpenAPI JSON document is available at:

https://url-shortener-service.duckdns.org/v3/api-docs

The Swagger UI documents URL creation, redirects, analytics, custom aliases, expiration support, validation errors, and rate limiting responses.

## Shorten API

Create a generated short URL:

```bash
curl -X POST https://url-shortener-service.duckdns.org/api/shorten \
  -H "Content-Type: application/json" \
  -d '{"originalUrl":"https://github.com"}'
```

Example response:

```json
{
  "shortCode": "1",
  "shortUrl": "https://url-shortener-service.duckdns.org/1"
}
```

Create a custom alias:

```bash
curl -X POST https://url-shortener-service.duckdns.org/api/shorten \
  -H "Content-Type: application/json" \
  -d '{"originalUrl":"https://github.com","customAlias":"github"}'
```

Example response:

```json
{
  "shortCode": "github",
  "shortUrl": "https://url-shortener-service.duckdns.org/github"
}
```

Create a URL that expires automatically:

```bash
curl -X POST https://url-shortener-service.duckdns.org/api/shorten \
  -H "Content-Type: application/json" \
  -d '{"originalUrl":"https://github.com","customAlias":"github-june","expiresAt":"2026-06-01T00:00:00"}'
```

`expiresAt` must be a future timestamp in ISO-8601 local date-time format.
Expired links return `410 Gone` instead of redirecting:

```json
{
  "error": "Short code has expired: github-june"
}
```

Custom aliases must be 3-30 characters and may contain only letters, numbers, hyphen, and underscore.
The reserved aliases `api`, `actuator`, and `analytics` are rejected.
If an alias is already taken, the API returns `409 Conflict`:

```json
{
  "error": "Alias is already taken: github"
}
```

## Analytics API

Fetch analytics for a short URL:

```bash
curl https://url-shortener-service.duckdns.org/api/analytics/{shortCode}
```

Example response:

```json
{
  "shortCode": "1",
  "originalUrl": "https://example.com",
  "clickCount": 3,
  "createdAt": "2026-05-21T12:00:00",
  "lastAccessedAt": "2026-05-21T12:05:00",
  "expiresAt": "2026-06-01T00:00:00",
  "expired": false
}
```
