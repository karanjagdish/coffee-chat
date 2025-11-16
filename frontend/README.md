# Coffee Chat Frontend (React + TypeScript + Vite)

Frontend UI for the Coffee Chat platform. Provides authenticated access to the User and Chat services (via the Nginx gateway) with a React + TypeScript SPA.

## Tech Stack

- React 18
- TypeScript 5
- Vite 7
- Tailwind CSS 4
- React Router 7
- Axios
- ESLint (type-aware config)

## Scripts

From the `frontend/` directory:

```bash
# Start dev server (http://localhost:5173 by default)
npm install
npm run dev

# Type-check and build for production
npm run build

# Lint all files
npm run lint

# Preview production build locally
npm run preview
```

## Environment & Backend Integration

- Backend services run via the `backend/` project (see root README).
- The frontend expects the Nginx gateway and backend to be reachable via the URLs configured in the backend `.env` / Docker Compose setup.

## Code Style

- Use **Prettier** and **ESLint** for formatting and linting.
- Keep types strict and prefer typed API clients for user, session, and message flows.
