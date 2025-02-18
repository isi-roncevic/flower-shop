import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class CustomerOrSupplier {
    public static void main(String[] args){
        try(
                //opening the socket and communications
                Socket socket=new Socket("localhost",Server.PORT);
                BufferedReader reader=new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter writer=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                Scanner sc=new Scanner(System.in)
        ) {

            //string used for storing the info about whether a client is a customer or a supplier
            String name;

            //question from the server to determine if this is customer or supplier
            System.out.println(reader.readLine());

            //response to the server
            //checking if the client has put "customer" or "supplier"
            while(true){
                name=sc.nextLine();
                if(!name.equalsIgnoreCase("customer") && !name.equalsIgnoreCase("supplier")) {
                    System.out.print("Please type 'customer' or 'supplier'\n");
                }
                else{
                    break;
                }
            }

            //giving the information about customer or supplier to the server
            Server.writing(name,writer);


            //the rest of the communication is done in a separate method
            communication(reader,writer,sc);


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void communication(BufferedReader reader, BufferedWriter writer, Scanner sc) throws IOException {

        //message received before listing all the flowers
        System.out.println(reader.readLine());
        //getting and printing the flowers available for customers or needed by suppliers
        String line;
        while(true){
            line = reader.readLine();
            if(line.equalsIgnoreCase("done")){
                break;
            }
            else{
                System.out.println(line);
            }
        }

        //question from the server about the type of flowers that the client would like to order/deliver
        System.out.println(reader.readLine());
        //response (writing names of all the flowers that the client wants/delivers, pressing enter between each, and typing 'end' at the end
        while(true) {
            line =sc.nextLine();
            if(line.equalsIgnoreCase("end")){
                Server.writing(line, writer);
                break;
            }
            else{
                Server.writing(line, writer);
            }
        }

        //if there is a flower that has a spelling error, or doesn't exit at all, the client will be asked to again write the flowers that exist
        while((line =reader.readLine()).contains("Please select one of the existing flowers.")){
            System.out.println(line);
            while(true) {
                line =sc.nextLine();
                if(line.equalsIgnoreCase("end")){
                    Server.writing(line, writer);
                    break;
                }
                else{
                    Server.writing(line, writer);
                }
            }
        }

        //question from the server about the quantity of flowers (the first question has to be separate because of the condition of the while above
        System.out.println(line);
        Server.writing(sc.nextLine(),writer);
        //response to the server
        while((line =reader.readLine()).contains("How many")){
            System.out.println(line);
            Server.writing(sc.nextLine(),writer);
        }

        //possible messages from the server, and adequate actions
        //the first message is connected to the customers exclusively
        if(line.contains("The following flowers are not available at the moment")){
            System.out.println(line);

            //question about the conformation of the order
            System.out.println(reader.readLine());

            //response to the server
            line = sc.nextLine();
            Server.writing(line,writer);

            if(line.equalsIgnoreCase("yes")){
                //conformation
                System.out.println(reader.readLine());
            }
            else{
                //question about starting order from the beginning
                System.out.println(reader.readLine());

                line =sc.nextLine();
                Server.writing(line,writer);
                if(line.equalsIgnoreCase("yes")){
                    //starting a new order
                    communication(reader,writer,sc);
                }
                else{
                    //conformation about ending the session
                    System.out.println(reader.readLine());
                }
            }

        }
        //the second message is also connected to the customer exclusively
        else if(line.contains("None of the flowers that you have requested are available in the quantities that you wish to order.")){
            System.out.println(line);

            //question about the conformation of the order
            System.out.println(reader.readLine());
            //response to the server
            line =sc.nextLine();
            Server.writing(line,writer);

            if(line.equalsIgnoreCase("no")){
                //conformation
                System.out.println(reader.readLine());
            }
            else{
                //starting a new order
                communication(reader,writer,sc);
            }

        }
        else{
            //this branch is tied to customer or supplier

            //question about the conformation of the order/delivery
            System.out.println(line);
            Server.writing(sc.nextLine(),writer);

            //conformation
            System.out.println(reader.readLine());
        }
    }
}
