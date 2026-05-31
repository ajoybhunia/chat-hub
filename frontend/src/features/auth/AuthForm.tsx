import React, { useState } from "react";
import { Box, Button, Tab, Tabs, TextField, Typography } from "@mui/material";
import { useAuthStore } from "../../store/auth.store";

export default function AuthForm() {
  const [tab, setTab] = useState(0);
  const [form, setForm] = useState({ username: "", email: "", password: "" });
  const [error, setError] = useState("");
  const { login, signup } = useAuthStore();

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    try {
      if (tab === 0) {
        await login(form.email, form.password);
      } else {
        await signup(form.username, form.email, form.password);
      }
    } catch (err: any) {
      setError(err.message || "Auth failed");
    }
  };

  return (
    <Box sx={{ maxWidth: 360, mx: "auto", mt: 8, p: 3, boxShadow: 2, borderRadius: 2 }}>
      <Tabs
        value={tab}
        onChange={(_, v) => setTab(v)}
      >
        <Tab label="Login" />
        <Tab label="Sign Up" />
      </Tabs>
      <form onSubmit={handleSubmit}>
        {tab === 1 && (
          <TextField
            margin="normal"
            fullWidth
            label="Username"
            name="username"
            value={form.username}
            onChange={handleChange}
            required
          />
        )}
        <TextField
          margin="normal"
          fullWidth
          label="Email"
          name="email"
          type="email"
          value={form.email}
          onChange={handleChange}
          required
        />
        <TextField
          margin="normal"
          fullWidth
          label="Password"
          name="password"
          type="password"
          value={form.password}
          onChange={handleChange}
          required
        />
        {error && (
          <Typography color="error" variant="body2" sx={{ mt: 1 }}>
            {error}
          </Typography>
        )}
        <Button type="submit" fullWidth variant="contained" sx={{ mt: 2 }}>
          {tab === 0 ? "Login" : "Sign Up"}
        </Button>
      </form>
    </Box>
  );
}
