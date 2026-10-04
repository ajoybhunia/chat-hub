export interface User {
  id: number;
  username: string;
  email: string;
}

export interface AuthResponse {
  user: User;
  token: string;
}
