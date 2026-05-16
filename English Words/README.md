# English Words Backend

Spring Boot backend for a vocabulary learning system. It supports JWT login, personal word books, CSV/Excel word import, random quizzes, SM-2 style progress updates, and Swagger API docs.

## Run

Create a MySQL database first:

```sql
CREATE DATABASE english_words DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Start the API:

```powershell
.\mvnw.cmd spring-boot:run
```

Default config uses:

- `DB_URL=jdbc:mysql://localhost:3306/english_words?...`
- `DB_USERNAME=root`
- `DB_PASSWORD=root`
- `JWT_SECRET=english-words-development-secret-change-me-before-production`

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

## Import File Format

CSV/Excel first row must contain:

```text
word,translation,phonetic,example,tags
```

Only `word` and `translation` are required. Duplicate words in the same user book update base word fields without resetting learning progress.

## Auth

Register or login returns:

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "JWT_TOKEN",
    "user": {}
  }
}
```

Send the token on business APIs:

```http
Authorization: Bearer JWT_TOKEN
```
