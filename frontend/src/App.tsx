import { Box } from "@mui/material";
import AuthForm from "./features/auth/AuthForm";
import ChatPanel from "./components/chat/ChatPanel";
import Sidebar from "./components/layout/Sidebar";
import { useAuthStore } from "./store/auth.store";

const App = () => {
  const { user } = useAuthStore();

  if (!user) {
    return <AuthForm />;
  }

  return (
    <Box sx={{ display: "flex", height: "100vh" }}>
      <Sidebar />
      <ChatPanel />
    </Box>
  );
};

export default App;
