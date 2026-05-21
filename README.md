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
