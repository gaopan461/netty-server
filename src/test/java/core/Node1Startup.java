package core;

/**
 * @author gaopan
 */
public class Node1Startup {

    public static void main(String[] args) throws InterruptedException {
        Node node = new Node("node1", 10011);
        node.startup();
        node.addRemoteNode("node2", "127.0.0.1", 10012);
        while (true) {
            node.pulse();
            node.sendMessage("node2", "gaopan");
            Thread.sleep(20);
        }
    }

}
