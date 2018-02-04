package Communication;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class User implements Serializable{
    private String name;
    private boolean isLogged;
    private HashMap<String, User> friendList;
    private transient InetAddress currentUsrAddr;
    private transient Socket mySocket;
    private transient int myPort;
    private String[] tmpFriendList;

    public void setTmpFriendList(String[] list) {
        this.tmpFriendList = list;
    }

    public String[] getTmpFriendList() {
        return tmpFriendList;
    }

    public User(String name, Socket mySocket){
        this.name = name;
        this.friendList = new HashMap<String, User>();
        this.mySocket = mySocket;
        // the user InetAddress
        this.currentUsrAddr = mySocket.getInetAddress();
        this.myPort = mySocket.getPort() + 1;
    }

    public User(String name){
        this.name = name;
        this.friendList = new HashMap<String, User>();
    }

    public User(){
        this.friendList = new HashMap<String, User>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMySocket(Socket sock){
        this.mySocket = sock;
        this.currentUsrAddr = sock.getInetAddress();
    }

    public Socket getMySocket() {
        return mySocket;
    }

    public boolean isFriendWith(String name){
        if (friendList.containsKey(name))
            return true;
        else
            return false;
    }

    // this is the port client and server will agree to connect on to exchange user messages
    // client opens a serversocket on this port
    // server attempts connection to this port
    public synchronized int getMyPort(){
        if(myPort == mySocket.getPort()) {
            myPort++;
        }
        if(myPort <= 65535) {
            int tmp = myPort;
            myPort++;
            return tmp;
        }
        else {
            myPort = 49153;
            return 49152;
        }
    }

    public HashMap<String, User> getFriendList() {
        return friendList;
    }

    // for direct communication between two clients when transferring files
    public InetAddress getCurrentUsrAddr() {
        return currentUsrAddr;
    }

    // requires the database of all users to work
    public void addFriend(String friendName, ConcurrentHashMap<String, User> Database){
        friendList.put(friendName, Database.get(friendName));
    }

    // only requires the key for which to search
    public void removeFriend(String friendName){
        Message reply;
        if(friendList.containsKey(friendName)){
            friendList.remove(friendName);
            reply = new Message("OP_PRT_MSG", friendName + " removed from your friendlist.");
        }
        else{
            reply = new Message("OP_PRT_MSG", "No such user in your friendlist.");
        }
        reply.send(mySocket);
    }

    // send list of friends to client as a string, each element separated by a comma
    public String transmitFriendList(){
        String[] myFriends = friendList.keySet().toArray(new String[friendList.size()]);
        StringBuilder sendFriends = new StringBuilder();
        for(String i : myFriends){
            sendFriends.append(i).append(",");
        }
        return sendFriends.toString();
    }

    public synchronized boolean isLogged() { return isLogged; }

    // TODO: RMI Callback for the other clients
    public synchronized boolean login(Socket sock){
        if(isLogged)
            return false;
        isLogged = true;
        this.mySocket = sock;
        this.currentUsrAddr = sock.getInetAddress();
        return true;
    }

    // TODO: RMI Callback for the other clients
    public synchronized void logout(){
        isLogged = false;
    }

}
