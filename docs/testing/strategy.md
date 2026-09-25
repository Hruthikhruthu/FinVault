# Testing Strategy

Backend verification uses:

- JUnit 5
- Mockito
- `@ParameterizedTest`
- `@WebMvcTest`
- `@DataJpaTest`
- Application-start integration test
- JaCoCo coverage check at 65% minimum for the common module

Commands:

```bash
cd backend
mvn verify
```

Frontend verification:

```bash
cd frontend
npm install
npm run build
npm audit --omit=dev
```
