
// defines pins numbers

/*
 id:
  0, 1: reverse parking assists

*/

const int trigPins[4] = {8, 9, 10, 11};
const int echoPins[4] = {4, 5, 6, 7};

const int soundpin= A0;
const int ledPin =13;
int clapCount=0;

long getDistFromSensor(int id){

  long duration;
  int distance;

  int trigPin = trigPins[id];
  int echoPin = echoPins[id];

   // Clears the trigPin
  digitalWrite(trigPin, LOW);
  delayMicroseconds(2);
  
  // Sets the trigPin on HIGH state for 10 micro seconds
  digitalWrite(trigPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(trigPin, LOW);
  
  // Reads the echoPin, returns the sound wave travel time in microseconds
  duration = pulseIn(echoPin, HIGH);
  
  // Calculating the distance
  distance= duration*0.034/2;
  return distance;
  
  
}

void setup() {
  for (int i=0; i<4; i++){
    pinMode(trigPins[i], OUTPUT); // Sets the trigPin as an Output
    pinMode(echoPins[i], INPUT); // Sets the echoPin as an Input
  }
pinMode(soundpin,INPUT);
pinMode(ledPin,OUTPUT); 
Serial.begin(9600); // Starts the serial communication
}

void loop() {
//int soundState = analogRead(soundpin); 
//Serial.println(soundState);
//  if (soundState > 500){
//
//    clapCount++; 
//
//    delay(500); 
//
//  }
//
//  if (clapCount ==4){
//
//    digitalWrite(ledPin, LOW); 
//
//    clapCount = clapCount % 2; 
//
//  }
//
//  if (clapCount == 2){
//
//    digitalWrite(ledPin, HIGH); 
//
//  }

  long distances[2];
  distances[0] = getDistFromSensor(0);
  distances[1] = getDistFromSensor(1);

  Serial.print(distances[0]);
  Serial.print(", ");
  Serial.println(distances[1]);

}
