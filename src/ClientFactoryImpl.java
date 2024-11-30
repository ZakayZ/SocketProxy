import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.Socket;

public class ClientFactoryImpl implements ClientFactory {
    private final String host;
    private final int port;

    public ClientFactoryImpl(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public <T> T newClient(Class<T> client) {
        validateService(client);
        return (T) Proxy.newProxyInstance(
                client.getClassLoader(),
                new Class<?>[]{client},
                new ClientHandler(host, port)
        );
    }

    private static class ClientHandler implements InvocationHandler {
        private final String host;
        private final int port;

        ClientHandler(String host, int port) {
            this.host = host;
            this.port = port;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            try (Socket socket = new Socket(host, port);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                out.writeUTF(method.getName());
                out.writeObject(args);

                var result = in.readObject();
                if (result instanceof Exception) {
                    throw (Exception) result;
                }

                return result;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }


    private <T> void validateService(Class<T> clazz) throws RuntimeException {
        for (var method : clazz.getMethods()) {
            if (!Serializable.class.isAssignableFrom(method.getReturnType())) {
                throw new RuntimeException("return type must be serializable");
            }
            for (var param : method.getParameterTypes()) {
                if (!Serializable.class.isAssignableFrom(param)) {
                    throw new RuntimeException("all parameters must be serializable");
                }
            }
        }
    }

}