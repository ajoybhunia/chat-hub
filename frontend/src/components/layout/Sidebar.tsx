// ...existing code...
import { Box, Button, Divider, Typography } from "@mui/material";
import { useAuthStore } from "../../store/auth.store";

export default function Sidebar() {
  const { user, logout } = useAuthStore();

  return (
    <Box
      sx={{
        width: 240,
        minWidth: 240,
        height: "100vh",
        bgcolor: "grey.900",
        color: "white",
        display: "flex",
        flexDirection: "column",
        p: 2,
        gap: 1,
      }}
    >
      <Typography variant="h6" sx={{ fontWeight: "bold", mb: 1 }}>
        Chat Hub
      </Typography>
      <Divider sx={{ borderColor: "grey.700" }} />

      <Typography variant="caption" color="grey.500" sx={{ mt: 1, textTransform: "uppercase", letterSpacing: 1 }}>
        Channels
      </Typography>
      <Box
        sx={{
          bgcolor: "grey.800",
          borderRadius: 1,
          px: 1.5,
          py: 0.75,
          cursor: "default",
        }}
      >
        <Typography variant="body2"># general</Typography>
      </Box>

      <Box sx={{ flex: 1 }} />
      <Divider sx={{ borderColor: "grey.700" }} />

      <Box sx={{ pt: 1 }}>
        <Typography variant="body2" color="grey.400" noWrap>
          {user?.username}
        </Typography>
        <Typography variant="caption" color="grey.600" noWrap>
          {user?.email}
        </Typography>
        <Button
          fullWidth
          size="small"
          variant="outlined"
          color="error"
          onClick={logout}
          sx={{ mt: 1 }}
        >
          Sign out
        </Button>
      </Box>
    </Box>
  );
}
