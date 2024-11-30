import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.Executors;

public class ServerFactoryImpl implements ServerFactory {
    private ServerSocket serverSocket;

    @Override
    public void listen(int port, Object service) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            serverSocket = new ServerSocket(port);

            for (; ; ) {
                var clientSocket = serverSocket.accept();
                executor.submit(() -> handleClient(clientSocket, service));
            }
        } catch (SocketException se) {
            return;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try {
            serverSocket.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handleClient(Socket clientSocket, Object service) {
        try (var in = new ObjectInputStream(clientSocket.getInputStream());
             var out = new ObjectOutputStream(clientSocket.getOutputStream())) {

            try {
                var methodName = in.readUTF();
                Object[] args = (Object[]) in.readObject();

                var method = findRequestedMethod(service, methodName, args);
                var result = method.invoke(service, args);
                out.writeObject(result);
            } catch (Exception e) {
                // notify about an error
                out.writeObject(e);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Method findRequestedMethod(Object service, String methodName, Object[] args) throws NoSuchMethodException {
        for (var method : service.getClass().getMethods()) {
            if (method.getName().equals(methodName) && isMethodFits(method.getParameterTypes(), args)) {
                return method;
            }
        }
        throw new NoSuchMethodException("No matching method found");
    }

    private boolean isMethodFits(Class<?>[] parameterTypes, Object[] args) {
        if ((args == null && parameterTypes.length != 0) || (args != null && parameterTypes.length != args.length)) {
            return false;
        }
        for (var i = 0; i < parameterTypes.length; ++i) {
            if (!parameterTypes[i].isAssignableFrom(args[i].getClass())) {
                return false;
            }
        }
        return true;
    }
}
