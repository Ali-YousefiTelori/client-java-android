package ir.atitec.signalgoApp;

public class MessageContract2<T> extends MessageContract {

    public MessageContract2() {

    }

    private T response;

    public T getResponse() {
        return response;
    }

    public void setResponse(T response) {
        this.response = response;
    }

}

