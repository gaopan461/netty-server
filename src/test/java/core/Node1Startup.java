package core;

/**
 * @author gaopan
 */
public class Node1Startup {

    public static void main(String[] args) throws InterruptedException {
        Node node = new Node("node1", 10011);
        node.startup();
        node.addRemoteNode("node2", "127.0.0.1", 10012);
        int count = 0;
        while (true) {
            node.pulse();
            node.sendMessage("node2", String.valueOf(count));
            Thread.sleep(1000);

            count++;
            if (count == 20) {
                node.delRemoteNode("node2");
            }
        }
    }

}
