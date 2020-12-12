/*
  AWS IoT WiFi

  This sketch securely connects to an AWS IoT using MQTT over WiFi.
  It uses a private key stored in the ATECC508A and a public
  certificate for SSL/TLS authetication.

  It publishes a message every 5 seconds to arduino/outgoing
  topic and subscribes to messages on the arduino/incoming
  topic.

  The circuit:
  - Arduino MKR WiFi 1010 or MKR1000

  The following tutorial on Arduino Project Hub can be used
  to setup your AWS account and the MKR board:

  https://create.arduino.cc/projecthub/132016/securely-connecting-an-arduino-mkr-wifi-1010-to-aws-iot-core-a9f365

  This example code is in the public domain.
*/

#include <ArduinoBearSSL.h>
#include <ArduinoECCX08.h>
#include <ArduinoMqttClient.h>
#include <WiFiNINA.h> // change to #include <WiFi101.h> for MKR1000
#include <Servo.h>
#include "arduino_secrets.h"

#define trash_trigPin 1
#define trash_echoPin 7
#define enter_trigPin 3
#define enter_echoPin 4
int motor_control = 6;
#include <ArduinoJson.h>
//#include "Led.h"
Servo servo;

/////// Enter your sensitive data in arduino_secrets.h
const char ssid[]        = SECRET_SSID;
const char pass[]        = SECRET_PASS;
const char broker[]      = SECRET_BROKER;
const char* certificate  = SECRET_CERTIFICATE;

WiFiClient    wifiClient;            // Used for the TCP socket connection
BearSSLClient sslClient(wifiClient); // Used for SSL/TLS connection, integrates with ECC508
MqttClient    mqttClient(sslClient);

unsigned long lastMillis = 0;


void setup() {
  Serial.begin(115200);
  while (!Serial);

  if (!ECCX08.begin()) {
    Serial.println("No ECCX08 present!");
    while (1);
  }
  ArduinoBearSSL.onGetTime(getTime);
  sslClient.setEccSlot(0, certificate);
  mqttClient.onMessage(onMessageReceived);
  pinMode(trash_trigPin, OUTPUT);
  pinMode(trash_echoPin, INPUT);
  pinMode(enter_trigPin, OUTPUT);
  pinMode(enter_echoPin, INPUT);
  servo.attach(motor_control);
}

char* top="close";
char* mod="user";

void loop() {
  if (WiFi.status() != WL_CONNECTED) {
    connectWiFi();
  }

  if (!mqttClient.connected()) {
    // MQTT client is disconnected, connect
    connectMQTT();
  }

  // poll for new MQTT messages and send keep alives
  mqttClient.poll();

    long enter_cm;
    enter_cm=find_enter_cm();
    Serial.print(enter_cm);
    
    if(strcmp(mod,"user")==0){// user 모드
      if(enter_cm<=10)//top_open->top_close
      {
        servo.write(120);//open
        
        top="open";
        char payload[512];
        getDeviceStatus(payload,mod,top);
        sendMessage(payload);
        delay(3000);
        servo.write(0);//
        
        top="close";
        getDeviceStatus(payload,mod,top);
        sendMessage(payload);
      }
      else //top_close
      {
        char payload[512];
        getDeviceStatus(payload,mod,top);
        sendMessage(payload);
      }
    }
    else if(strcmp(mod,"admin")==0){ // admin 모드
      
      Serial.println("admin_stop");
      char payload[512];
      getDeviceStatus(payload,mod,top);
      sendMessage(payload);
    }
    delay(2000);
    
}

void getDeviceStatus(char* payload, char* mod, char* top) {
    long trash_cm;
    trash_cm=find_trash_cm();
    sprintf(payload,"{\"state\":{\"reported\":{\"mod\":\"%s\",\"top\":\"%s\",\"trash_cm\":\"%ld\"}}}",mod,top,trash_cm);
}

long microsecondsToCentimeters(long microseconds)
{
  return microseconds / 29 / 2;
}

long find_enter_cm()
{
  long duration, enter_cm;
  digitalWrite(enter_trigPin, LOW);
  delayMicroseconds(2);
  digitalWrite(enter_trigPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(enter_trigPin, LOW);
  duration = pulseIn(enter_echoPin, HIGH);
  // convert the time into a distance
  enter_cm = microsecondsToCentimeters(duration);
  return enter_cm;
  
}

long find_trash_cm()
{
  long duration, trash_cm;
  digitalWrite(trash_trigPin, LOW);
  delayMicroseconds(2);
  digitalWrite(trash_trigPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(trash_trigPin, LOW);
  duration = pulseIn(trash_echoPin, HIGH);
  // convert the time into a distance
  trash_cm = microsecondsToCentimeters(duration);
  return trash_cm;
  
}



void sendMessage(char* payload) {
  char TOPIC_NAME[]= "$aws/things/MyMKRWiFi1010/shadow/update";
  
  Serial.print("Publishing send message:");
  Serial.println(payload);
  mqttClient.beginMessage(TOPIC_NAME);
  mqttClient.print(payload);
  mqttClient.endMessage();
}

unsigned long getTime() {
  // get the current time from the WiFi module  
  return WiFi.getTime();
}

void connectWiFi() {
  Serial.print("Attempting to connect to SSID: ");
  Serial.print(ssid);
  Serial.print(" ");

  while (WiFi.begin(ssid, pass) != WL_CONNECTED) {
    // failed, retry
    Serial.print(".");
    delay(5000);
  }
  Serial.println();

  Serial.println("You're connected to the network");
  Serial.println();
}

void connectMQTT() {
  Serial.print("Attempting to MQTT broker: ");
  Serial.print(broker);
  Serial.println(" ");

  while (!mqttClient.connect(broker, 8883)) {
    // failed, retry
    Serial.print(".");
    delay(5000);
  }
  Serial.println();

  Serial.println("You're connected to the MQTT broker");
  Serial.println();

  // subscribe to a topic
  mqttClient.subscribe("$aws/things/MyMKRWiFi1010/shadow/update/delta");
}

void onMessageReceived(int messageSize) {
  // we received a message, print out the topic and contents
  Serial.print("Received a message with topic '");
  Serial.print(mqttClient.messageTopic());
  Serial.print("', length ");
  Serial.print(messageSize);
  Serial.println(" bytes:");

  // store the message received to the buffer
  char buffer[512] ;
  int count=0;
  while (mqttClient.available()) {
     buffer[count++] = (char)mqttClient.read();
  }
  buffer[count]='\0'; // 버퍼의 마지막에 null 캐릭터 삽입
  Serial.println(buffer);
  Serial.println();

  // JSon 형식의 문자열인 buffer를 파싱하여 필요한 값을 얻어옴.
  // 디바이스가 구독한 토픽이 $aws/things/MyMKRWiFi1010/shadow/update/delta 이므로,
  // JSon 문자열 형식은 다음과 같다.
  // {
  //    "version":391,
  //    "timestamp":1572784097,
  //    "state":{
  //        "LED":"ON"
  //    },
  //    "metadata":{
  //        "LED":{
  //          "timestamp":15727840
  //         }
  //    }
  // }
  //
  DynamicJsonDocument doc(1024);
  deserializeJson(doc, buffer);
  JsonObject root = doc.as<JsonObject>();
  JsonObject state = root["state"];
  const char* a = state["mod"];
  Serial.println(a);
  
  char payload[512];
  if (strcmp(a,"user")==0) {
    servo.write(0);
    mod="user";//mod의 상태 변경
    //sprintf(payload,"{\"state\":{\"reported\":{\"mod\":\"%s\"}}}",mod);
    //sendMessage(payload);
    top="close";
    
  } else if (strcmp(a,"admin")==0) {
    servo.write(120);
    mod="admin";//mod의 상태 변경
    //sprintf(payload,"{\"state\":{\"reported\":{\"mod\":\"%s\"}}}",mod);
    //sendMessage(payload);
    top="open";
  }

 
}
