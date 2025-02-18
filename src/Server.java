import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;


public class Server {
    public static final int PORT = 1234;
    //map for all the flowers and their quantities
    private static final Map<String,Integer> flowers=new TreeMap<>();
    //list for the flowers available to the customer or supplier
    private static final List<String> theListedFlowers=new ArrayList<>();

    public static void main(String[] args) {
        try {
            //getting all the flowers from the txt file
            loadTheFlowers();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (
                //opening server socket
                ServerSocket serverSocket = new ServerSocket(PORT)

        ) {

            System.out.println("Listening for customers or suppliers");
            while (true) {
                //constantly listening for new clients

                Socket customerOrSupplier = serverSocket.accept();
                System.out.println("Accepted!");

                //starting a thread for every client
                new Thread(new CustomerOrSupplierHandlerRunnable(customerOrSupplier)).start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //loading the flowers and their number in the map
    private static void loadTheFlowers() throws IOException {
        BufferedReader reader=new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get("C:/Users/korisniiiiik/IdeaProjects/flower-shop/src/flowers.txt"))));
        String flower;
        int numberOfFlowers;
        Random random=new Random();
        while((flower=reader.readLine())!=null){
            //adds a random number of flowers to a certain flower
            numberOfFlowers=random.nextInt(100);
            flowers.put(flower.toLowerCase(),numberOfFlowers);
        }
    }

    //method for writing
    public static void writing(String message, BufferedWriter writer) throws IOException {
        writer.write(message);
        writer.newLine();
        writer.flush();
    }

    //method for listing all the flowers
    synchronized public static void printTheFlowers(BufferedWriter writer, String name) throws IOException {
        for(Map.Entry<String,Integer> flower:flowers.entrySet()){
            //writing all the flowers available to the customers and remembering them in a list
            if(name.equalsIgnoreCase("customer") && flower.getValue()>0) {
                writing(flower.getKey()+" "+flower.getValue(), writer);
                theListedFlowers.add(flower.getKey());
            }
            //writing all the flowers needed by the supplier and remembering them in a list
            else if(name.equalsIgnoreCase("supplier") && flower.getValue()<10){
                writing(flower.getKey()+" "+flower.getValue(), writer);
                theListedFlowers.add(flower.getKey());
            }
        }
        //signal for the end of printing
        writing("Done",writer);
    }


    //method for checking if there is a requested number of a certain flower available
    synchronized public static boolean checkFlowerAvailability(String requestedFlower, Integer numberOfFlowers) {
        Integer checker=flowers.get(requestedFlower);
        return checker >= numberOfFlowers;
    }

    //methods for changing the number of flowers in an original map after confirming the order or delivery
    synchronized public static void changingTheFlowerNumbers(String requestedFlower, Integer numberOfFlowers, String name) {
        if(name.equalsIgnoreCase("customer")){
            flowers.compute(requestedFlower,(k,oldNumOfFlower)->oldNumOfFlower-=numberOfFlowers);
        }
        else{
            flowers.compute(requestedFlower, (k, oldNumofflowers) -> oldNumofflowers+=numberOfFlowers);
        }
    }

    //method for checking if the flower exists in the list of all flowers
    public static boolean checkFlowerName(String requestedFlower) {
        return theListedFlowers.contains(requestedFlower.toLowerCase());
    }
}
