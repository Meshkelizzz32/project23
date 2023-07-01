package project27;
import java.util.*;
import java.util.concurrent.*;

class WebClient {
 private final int serviceTime;
 public WebClient(int tm) { serviceTime = tm; }
 public int getServiceTime() { return serviceTime; }
 public String toString() {
 return "[" + serviceTime + "]";
 }
}
class WebClientLine extends ArrayBlockingQueue<WebClient> {
 public WebClientLine(int maxLineSize) {
 super(maxLineSize);
 }
 public String toString() {
 if(this.size() == 0)
 return "[Empty]";
 StringBuilder result = new StringBuilder();
 for(WebClient client : this)
 result.append(client);
 return result.toString();
 }
}
class WebClientGenerator implements Runnable {
private WebClientLine clients;
 volatile int loadFactor = 1; 
 private static Random rand = new Random(47);
 public WebClientGenerator(WebClientLine cq) {
 clients = cq;
 }
 public void run() {
 try {
 while(!Thread.interrupted()) {
 clients.put(new WebClient(rand.nextInt(1000)));
 TimeUnit.MILLISECONDS.sleep(1000 / loadFactor);
 }
 } catch(InterruptedException e) {
	 System.out.println("WebClientGenerator interrupted");
 }
     System.out.println("WebClientGenerator terminating");
 }
}
class Server implements Runnable {
 private static int counter;
 private final int id = counter++;
 private WebClientLine clients;
 public Server(WebClientLine cq) { clients = cq; }
 public void run() {
 try {
 while(!Thread.interrupted()) {
 WebClient client = clients.take();
 TimeUnit.MILLISECONDS.sleep(
 client.getServiceTime());
 }
 } catch(InterruptedException e) {
	 System.out.println(this + "interrupted");
 }
 System.out.println(this + "terminating");
 }
 public String toString() { return "Server " + id + " "; }
 public String shortString() { return "T" + id; }
}
class SimulationManager implements Runnable {
 private ExecutorService exec;
 private WebClientGenerator gen;
 private WebClientLine clients;
 private Queue<Server> servers =
 new LinkedList<Server>();
 private int adjustmentPeriod;

 private boolean stable = true;
 private int prevSize;
 public SimulationManager(ExecutorService e,
 WebClientGenerator gen, WebClientLine clients,
 int adjustmentPeriod, int n) {
 exec = e;
 this.gen = gen;
 this.clients = clients;
 this.adjustmentPeriod = adjustmentPeriod;
 for(int i= 0; i < n; i++) {
 Server server = new Server(clients);
 exec.execute(server);
 servers.add(server);
 }
 }
 public void adjustLoadFactor() {
 if(clients.size() > prevSize) {
 if(stable) 
 stable = false;
 else if(!stable) {
 System.out.println("Peak load factor: ~" + gen.loadFactor);
 exec.shutdownNow();
 }
 } else {
	 System.out.println("New load factor: " + ++gen.loadFactor);
 stable = true;
 }
 prevSize = clients.size();
 }
 public void run() {
 try {
 while(!Thread.interrupted()) {
 TimeUnit.MILLISECONDS.sleep(adjustmentPeriod);
 System.out.print(clients + " { ");
 for(Server server : servers)
	 System.out.println(server.shortString() + " ");
 System.out.println("}");
 adjustLoadFactor();
 }
 } catch(InterruptedException e) {
	 System.out.println(this + "interrupted");
 }
 System.out.println(this + "terminating");
 }
 public String toString() { return "SimulationManager "; }
}
public class WebClientServerSimulation {
 static final int MAX_LINE_SIZE = 50;
 static final int NUM_OF_SERVERS = 3;
 static final int ADJUSTMENT_PERIOD = 1000;
 public static void main(String[] args) throws Exception {
 ExecutorService exec = Executors.newCachedThreadPool();
 WebClientLine clients =
 new WebClientLine(MAX_LINE_SIZE);
 WebClientGenerator g = new WebClientGenerator(clients);
 exec.execute(g);
 exec.execute(new SimulationManager(
 exec, g, clients, ADJUSTMENT_PERIOD, NUM_OF_SERVERS));
 if(args.length > 0) 
 TimeUnit.SECONDS.sleep(new Integer(args[0]));
 else {
 System.out.println("Press 'ENTER' to quit");
 System.in.read();
 }
 exec.shutdownNow();
 }
}