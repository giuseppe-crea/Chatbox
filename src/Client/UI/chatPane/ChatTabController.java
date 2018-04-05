package Client.UI.chatPane;

import Client.UI.Controller;
import Client.UI.TestUI;
import Communication.Message;
import javafx.fxml.FXML;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

import static Client.UI.TestUI.myUser;

public class ChatTabController {
    private String mode, myName = TestUI.myUser.getName(), udpNameField;
    private Socket chatSocket;
    private DatagramSocket udpChatSocket;
    private InetAddress address= myUser.getMySocket().getInetAddress();
    private int portIn, udpNameLength, udpTextFieldSize;

    @FXML
    private javafx.scene.control.TextArea visualizingTextAreaItem, typingTextAreaItem;
    @FXML
    private javafx.scene.control.Button sendButton;
    @FXML
    private javafx.scene.text.Text characterCounter;

    public void setChatSocket(Socket sock){
        this.chatSocket = sock;
        this.mode = "tcp";
        characterCounter.setText("0/500");
    }

    public void setUdpChatSocket(DatagramSocket udpChatSocket, int portIn) {
        this.udpChatSocket = udpChatSocket;
        this.mode = "udp";
        this.portIn = portIn;
        this.udpNameField = "<" + myName + ">: ";
        try {
            this.udpNameLength = udpNameField.getBytes("UTF-8").length;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        this.udpTextFieldSize = 512 - udpNameLength;
        characterCounter.setText("0/"+udpTextFieldSize);
    }

    public void addLine(String username, String content){
        String[] contents = content.split("\n");
        for(String line: contents) {
            visualizingTextAreaItem.appendText("<" + username + "> " + line + "\n");
        }
    }

    public void addLine(String content){
        String[] contents = content.split("\n");
        for(String line: contents) {
            visualizingTextAreaItem.appendText(line + "\n");
        }
    }

    public void countCharacters(){
        String tmp = typingTextAreaItem.getText().trim();
        if(mode.equals("tcp")){
            // up to 500 bytes name excluded
            int byteCount = tmp.getBytes().length;
            characterCounter.setText(byteCount+"/500");
        }else if(mode.equals("udp")){
            // up to 512 including name
            try {
                int byteCount = tmp.getBytes("UTF-8").length;
                characterCounter.setText(byteCount+"/"+udpTextFieldSize);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

        }
    }

    public void typeLine(){
        String tmp;
        if(mode.equals("tcp")) {
            if ((tmp = typingTextAreaItem.getText().trim()) != null && !tmp.equals("") && tmp.getBytes().length < 500) {
                this.addLine(myName, tmp);
                Message sendLine = new Message("OP_FRD_CHT_MSG", tmp);
                try {
                    sendLine.send(chatSocket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                typingTextAreaItem.clear();
                typingTextAreaItem.positionCaret(0);
            }
        }else {
            StringBuilder tmpBuilder = new StringBuilder();
            try {
                if ((tmp = typingTextAreaItem.getText().trim()) != null && !tmp.equals("")) {
                    tmpBuilder.append(udpNameField);
                    tmpBuilder.append(tmp);
                    String sendString = tmpBuilder.toString();
                    if(sendString.getBytes("UTF-8").length < 512) {
                        DatagramPacket p = new DatagramPacket(
                                sendString.getBytes("UTF-8"), 0,
                                sendString.getBytes("UTF-8").length,
                                address, portIn);
                        udpChatSocket.send(p);
                        typingTextAreaItem.clear();
                        typingTextAreaItem.positionCaret(0);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void keyListener(KeyEvent event){
        if(event.getCode() == KeyCode.ENTER) {
            typeLine();
        }
    }

    public void lockWrite() {
        typingTextAreaItem.setEditable(false);
        typingTextAreaItem.setDisable(true);
        sendButton.setDisable(true);
    }

    public void onClose(String username) {
        Message msg = new Message();
        if(mode.equals("tcp")) {
            System.out.println("TCP tab " + username + " closing.");
            msg.setFields("OP_END_CHT", username);
        }else if (mode.equals("udp")){
            System.out.println("UDP tab " + username + " closing.");
            if(Controller.openGroupChats.containsKey(username)) {
                Controller.openGroupChats.replace(username, false);
                System.out.println("*******************3");
                System.out.println("Replaced with false for group chat " + username);
            }else{
                System.out.println("THERE WAS NO SUCH CHAT!");
            }
            msg.setFields("OP_LEV_GRP", username);
        }
        try {
            msg.send(TestUI.myUser.getMySocket());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
