import java.io.*;
import java.net.Socket;
import java.util.*;

public class CustomerOrSupplierHandlerRunnable implements Runnable {
    private final Socket client;
    //string used for storing the info about the client (if he is a customer or a supplier)
    private String name;
    //lists used for checking different aspects of flowers (if the name is correct, which flowers are being ordered, if there are enough flowers
    private final List<String> goodFlowers=new ArrayList<>();
    private final List<String> badFlowers=new ArrayList<>();
    //map used for which flowers and how many of them are being ordered
    private final Map<String,Integer> flowersAndNumber=new TreeMap<>();
    //string used for printing the words 'order' or 'deliver' depending on whom the client is
    private String orderOrDeliver;

    //constructor
    public CustomerOrSupplierHandlerRunnable(Socket customerOrSupplier) {
        this.client = customerOrSupplier;
    }

    @Override
    public void run() {
        try (
                //opening reader and writer for communication
                BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()))
        ) {

            //asking a client what he is and getting an answer
            Server.writing("Are you a customer or a supplier?", writer);
            name = reader.readLine();

            //based on what the client is, setting the orderOrDeliver string
            orderOrDeliver=name.equalsIgnoreCase("customer")?"order":"deliver";

            //the rest of the communication is done in a separate method
            communication(reader,writer);

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    private void communication(BufferedReader reader,BufferedWriter writer) throws IOException {

        //listing all the flowers depending on whether a client is a customer or supplier
        if(name.equalsIgnoreCase("customer")) {
            Server.writing("The flowers we offer are:", writer);
        }
        else{
            Server.writing("The flowers we need are:", writer);
        }
        Server.printTheFlowers(writer,name);

        //asking a client for the list of flowers they would like to order/deliver
        Server.writing("Which flowers would you like to "+orderOrDeliver+"? (after typing the name of the flower, press enter, when" +
                "you are done listing the flowers, type 'end')",writer);

        //getting an answer (checking for each flower if the flower exists, if it does, it's added to the goodFlowers, if not, badFlowers)
        while(true) {
            //string for loading names of flowers
            String flowers = reader.readLine().trim();
            if (!(flowers.equalsIgnoreCase("end"))) {
                if (Server.checkFlowerName(flowers)) {
                    if(!goodFlowers.contains(flowers)){
                        goodFlowers.add(flowers.toLowerCase());
                    }
                } else {
                    badFlowers.add(flowers);
                }
            } else {
                //if there are no bad flowers, we are exiting the loop
                if(badFlowers.isEmpty()){
                    break;
                }
                //notify the client about the flowers that are non-existing or wrongly spelled, clear the list of badFlowers, and continue the loop
                else{
                    Server.writing("Please select one of the existing flowers. The following don't exist or are not available: "+toString(),writer);
                    badFlowers.clear();
                }
            }
        }

        //asking the client how many of the flowers he wants to order/deliver, and putting the number and the name in a map
        for(String flower:goodFlowers) {
            Server.writing("How many " + flower + " would you like to "+orderOrDeliver+"?", writer);
            Integer numberOfFlowers = Integer.valueOf(reader.readLine());
            flowersAndNumber.put(flower,numberOfFlowers);
        }

        //opening different methods for working with customer or supplier for the remaining part of the communication
        if(name.equalsIgnoreCase("customer")){
            customerCommunicationMethod(reader,writer);
        }
        else{
            supplierCommunicationMethod(reader,writer);
        }
    }

    //method for working with a supplier
    private void supplierCommunicationMethod(BufferedReader reader, BufferedWriter writer) throws IOException {

        //asking supplier to confirm the delivery
        Server.writing("Are you sure you would like to "+orderOrDeliver+" selected flowers and quantities? (type yes/no)",writer);

        //updating the number of flowers in an original map of flowers and quantities available
        if (reader.readLine().equalsIgnoreCase("yes")) {
            for (Map.Entry<String, Integer> flower : flowersAndNumber.entrySet()) {
                Server.changingTheFlowerNumbers(flower.getKey(), flower.getValue(), name);
            }
            Server.writing("You have successfully delivered. Thank you!", writer);
        } else if (reader.readLine().equalsIgnoreCase("no")) {
            Server.writing("You have terminated your delivery.", writer);
        }

    }

    //method for working with a customer
    private void customerCommunicationMethod(BufferedReader reader, BufferedWriter writer) throws IOException {

        //clearing the lists for new use
        goodFlowers.clear();
        badFlowers.clear();

        //checking if all the requested flowers and quantities are available at the moment, and separating them into lists
        for(Map.Entry<String,Integer> flower:flowersAndNumber.entrySet()){
            if(!Server.checkFlowerAvailability(flower.getKey(),flower.getValue())){
                badFlowers.add(flower.getKey());
            }
            else{
                goodFlowers.add(flower.getKey());
            }
        }

        if(goodFlowers.isEmpty()){
            //if there are no flowers available at the moment, notify the customer
            Server.writing("None of the flowers that you have requested are available in the quantities that you wish to order.",writer);
            //ask the customer about starting a new order
            Server.writing("Would you like to start your order from the beginning? (type yes/no)",writer);
            if(reader.readLine().equalsIgnoreCase("yes")){
                //clearing all the lists and maps for new use
                goodFlowers.clear();
                badFlowers.clear();
                flowersAndNumber.clear();
                //starting a new order
                communication(reader,writer);
            }
            else{
                //if the answer is no, ending a session
                Server.writing("You have successfully ended your session. Thank you!", writer);
            }
        }
        else if(!badFlowers.isEmpty()){
            //if there are certain flowers that are not available, list them to the customer
            Server.writing("The following flowers are not available at the moment "+toString(),writer);

            //asking the customer to confirm the delivery
            Server.writing("Are you sure you would like to order selected flowers and quantities that are available? (type yes/no)", writer);

            //updating the number of flowers in an original map of flowers and quantities available
            if (reader.readLine().equalsIgnoreCase("yes")) {
                for (String flower : goodFlowers) {
                    Server.changingTheFlowerNumbers(flower, flowersAndNumber.get(flower), name);
                }
                Server.writing("You have successfully ordered. Thank you!", writer);
            } else {
                //if the answer is no, ask a customer about starting a new order
                Server.writing("Would you like to start your order from the beginning? (type yes/no)", writer);
                if(reader.readLine().equalsIgnoreCase("yes")){
                    //clearing all the lists and maps for new use
                    badFlowers.clear();
                    goodFlowers.clear();
                    flowersAndNumber.clear();
                    //starting a new order
                    communication(reader,writer);
                }
                else{
                    //if the answer is no, ending a session
                    Server.writing("You have successfully ended your session. Thank you!",writer);
                }
            }
        }
        else{
            //if everything is available, notify the customer and ask them to confirm the order
            Server.writing("Are you sure you would like to order selected flowers and quantities that are available? (type yes/no)", writer);


            //updating the number of flowers in an original map of flowers and quantities available
            if (reader.readLine().equalsIgnoreCase("yes")) {
                for (String flower : goodFlowers) {
                    Server.changingTheFlowerNumbers(flower, flowersAndNumber.get(flower), name);
                }
                Server.writing("You have successfully ordered. Thank you!", writer);
            } else {
                Server.writing("You have terminated your order.", writer);
            }

        }
    }

    @Override
    public String toString() {
        StringBuilder builder=new StringBuilder();
        builder.append("*");
        for(String flower:badFlowers){
            builder.append(flower);
            builder.append("*");
        }
        return builder.toString();
    }

    private void yesOrNo(BufferedReader reader, BufferedWriter writer) {
    }
}
