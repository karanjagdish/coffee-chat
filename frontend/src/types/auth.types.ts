export interface User {
  id: string
  username: string
  email: string
  createdAt: string
}

export interface AuthPayload {
  token: string
  refreshToken: string
  apiKey: string
  user: User
  expiresIn: number
}

export interface LoginRequest {
  usernameOrEmail: string
  password: string
}

export interface SignupRequest {
  username: string
  email: string
  password: string
}

export interface RefreshTokenRequest {
  refreshToken: string
}
