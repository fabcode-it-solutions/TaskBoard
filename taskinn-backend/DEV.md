# Endpoint security

```sql
@RequireRole(Role.CUSTOMER)
```

or

```sql
@RequireRoles(
    value = {
        @RequireRole(Role.ADMIN),
        @RequireRole(Role.FRONTDESK),
    }
)
```

or

```sql
@RequireRoles(
    value = {
        @RequireRole(Role.ADMIN),
        @RequireRole(Role.FRONTDESK),
    },
    requireAll = true
)
```

- all work at both class and method level

### Simulate prod / dev environment
To simulate the prod / dev environment locally, those steps must be followed:

If you want to include a frontend build, build the frontend with:

`npm run build`

Then copy the content of `dist/taskinn-frontend/browser` to `src/main/resources/static/` (or for dev deployment add `dev/`)

Build the backend with:

`mvn clean package -DskipTests`

Build the docker image with jib:

`mvn -B jib:dockerBuild -Dimage=taskinn-backend:local`

Run the docker container with:

`docker run --rm -p 8080:8080 taskinn-backend:local`

#### Additional network setup
To simulate proper network setup, for investigating CORS issues, etc., you can run a reverse proxy like nginx in another container:

Add a file `nginx.conf` with the following content:

```nginx
events {}

http {
    server {
        listen 80;

        # simulate your real ingress path, e.g. /dev/
        location /dev/ {
            proxy_pass http://host.docker.internal:8080/dev/;
            proxy_set_header Host $host;
        }
    }
}
```

And run the container with:
```bash
docker run --rm -p 9999:80 \
  -v $(pwd)/nginx.conf:/etc/nginx/nginx.conf:ro \
  nginx
```

The backend is then reachable under port `9999`, the frontend under:

`http://localhost:9999/dev/`

State: 10.12.2025
