# English Words Deployment

This project is deployed as two services:

- Frontend: Vercel, root directory `English Words Frontend`
- Backend and MySQL: Railway, backend root directory `English Words`

## 1. Prepare GitHub

Create a GitHub repository from `D:\Demo\Annual Project` so both project folders stay in one repository.

Do not commit local secrets. The root `.gitignore` excludes `.env`, `node_modules`, `dist`, and `target`.

From PowerShell:

```powershell
cd "D:\Demo\Annual Project"
git init
git add .
git commit -m "Prepare production deployment"
git branch -M main
git remote add origin https://github.com/<your-github-user>/<your-repo-name>.git
git push -u origin main
```

If Git asks you to log in, follow GitHub's browser/device login flow. Replace `<your-github-user>` and `<your-repo-name>` with the repository you create on GitHub.

## 2. Deploy Backend on Railway

1. Create a Railway project from the GitHub repository.
2. Add a MySQL service.
3. Add a backend service from the same repo and set its root directory to `English Words`.
4. Generate a Railway public domain for the backend service.
5. Set backend variables:

```text
DB_URL=jdbc:mysql://<MYSQLHOST>:<MYSQLPORT>/<MYSQLDATABASE>?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8
DB_USERNAME=<MYSQLUSER>
DB_PASSWORD=<MYSQLPASSWORD>
JWT_SECRET=<replace-with-at-least-32-random-characters>
JWT_EXPIRATION_MS=86400000
```

Use the values provided by Railway's MySQL service for `<MYSQLHOST>`, `<MYSQLPORT>`, `<MYSQLDATABASE>`, `<MYSQLUSER>`, and `<MYSQLPASSWORD>`.

Railway provides `PORT` automatically. The backend reads it through `server.port=${PORT:8080}`.

## 3. Deploy Frontend on Vercel

1. Import the GitHub repository into Vercel.
2. Set Root Directory to `English Words Frontend`.
3. Use:
   - Build Command: `npm run build`
   - Output Directory: `dist`
4. Set this production environment variable:

```text
VITE_API_BASE_URL=https://<your-railway-backend-domain>/api
```

Redeploy after changing `VITE_API_BASE_URL`. Vite injects `VITE_` variables at build time.

## 4. Smoke Test

After both services deploy:

1. Open the Vercel URL and confirm the login page loads.
2. Register a new account.
3. Create a word book and add at least four words.
4. Open `/review` and complete one review question.
5. Refresh the page and confirm the login state persists.
6. Visit `https://<your-railway-backend-domain>/swagger-ui/index.html` to confirm the backend is reachable.

## Local Verification

Frontend:

```powershell
cd "D:\Demo\Annual Project\English Words Frontend"
npm run build
```

Backend:

```powershell
cd "D:\Demo\Annual Project\English Words"
.\mvnw.cmd test
```
