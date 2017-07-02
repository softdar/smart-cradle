//pin13
int LEDPin = 13;      //pin digital del led para mostrar acciones
int LEDEncendido = 9; //pin digital para el led que muestra si el sistema esta encendido o no

//temperatura
int Temperatura = A1 ; //pin analogico del sensor de temperatura
int tempMin=23;       //parametro de temperatura minima
int tempMax=25;       //parametro de temperatura maxima

//sensor de sonido
int rangoSonido; //es el umbral que usa para activar o no
int sonidoPin = A0;   //pin analogico que utiliza el sensor de sonido
int volumen;          //valor del volumen que capta el sensor

//buzzer
const int buzzer = 10; //pin digital que usa el buzzer

//PIR
const int PIRPin= 11;  //pin digital que usa el sensor de movimientos

//TILT
int TiltPin = 12;     //pin digital donde esta conectado el TILT
int lecturaTilt;      //guarda la lectura del valor del sensor
int previoTilt = LOW;   //valor inicial del sensor
long time = 0;            // Para guardar la hora de la inversion de valor
long debounce = 50;       // Tiempo de rebote
int  tiltActivado=0;      //Se utiliza para saber si el TILT esta activado o no

//boton
int pulsador = LOW;     //estado inicial del boton
int encendido=0;        //bandera que se utiliza para ver si el sistema esta activado o no
int pinBoton=7;         //pin digital que usa el boton
  
//bluetooth
#include <SoftwareSerial.h>   //libreria que maneja el bluetooth
SoftwareSerial BT1(4, 3); // se define el RX | TX  
String cadena;        //guarda la cadena de string que recibe del bluetooth

void setup()
{
   Serial.begin(9600);          //inicializa el serial
   pinMode(LEDPin,OUTPUT);       //LED 13
   pinMode(LEDEncendido,OUTPUT);       //LED encendido
   pinMode(buzzer, OUTPUT);   //buzzer
   pinMode(PIRPin, INPUT);    //PIR
   pinMode(pinBoton, INPUT);         //BOTON
   pinMode(TiltPin, INPUT_PULLUP);   //inicializa TILT 

   //bluetooth
  BT1.begin(9600);  //inicializa el bluetooth

}
void loop()
{

  //mientras reciba datos del bllluetooth, lo va guardando para luego analizarlo
  while (BT1.available()) {
       char c = BT1.read();  //lee el buffer de bluetooth, caracter a caracter
      if (c == '\n') {
        break;  //cuando encuentra el salto de linea, da por finalizada la lectura del string
      }

      //va guardando los caracteres en el string
      cadena += c;
  } 

   //si la cadena tiene valores, procede a analizar lo que recibio 
  if (cadena.length() >0 ) {

      //si la cadena empieza con T, lo que se recibe es datos de temperatura      
      if (cadena[0]=='T'){
        //extrae la temperatura minima que se envia desde el android
        String aux=cadena.substring(1,3);
        //asiga el valor a la temperatura minima
        tempMin=aux.toInt();
        //extrae la temperatura maxima que se envia desde el android
        aux=cadena.substring(4,6);
        //asiga el valor a la temperatura maximo
        tempMax=aux.toInt();
      }else if (cadena[0]=='1' and encendido==0){//si envia uno y no esta encendido, enciende el equipo
        encendido=1;//activa la bandera que dice que el sistema esta encedito
        sonido();//calcula el rango del sensor de sonido
        delay(200);
      }else if (cadena[0]=='0' and encendido==1){//si envia 0 y no esta apagado,apaga el equipo
        encendido=0;//activa la bandera que dice que el sistema esta apagado        
        delay(200);
      }
      cadena=""; //clears variable for new input
    }
    
  //boton  de encendido, enciende y apaga el sistema
  pulsador = digitalRead(pinBoton);//lee el estado del botón
  if(pulsador==HIGH and encendido==0){//enciende el sistema
    encendido=1;//activa la bandera que dice que el sistema esta encendido
    sonido();//calcula el rango del sensor de sonido
    delay(200);
  }else if(pulsador==HIGH and encendido==1){//apaga el sistema
    encendido=0;//pone en 0 la bandera que indica si el sistema esta activado o no
    delay(200);
  } 
 
  //si el sistema se encuentra encendido hace todo los controles de los sensores  
  if(encendido==1) {//si el estado es pulsado y estaba apagado, se enciende
    digitalWrite(LEDEncendido, HIGH);//se enciende el led

  //comienza la logica del sensor de temperatura
  //si supera los 25 o baja los 24 enciende una luz
  int lectura = analogRead(Temperatura);
  float voltaje = 5.0 /1024 * lectura ; 
  float temp = voltaje * 100 -50 ; 
  if (temp< tempMin or temp > tempMax) {
    digitalWrite(LEDPin, HIGH) ;//enciende el led, que significa que activo el AC 
    BT1.println(2);//envia por bluetooth la accion que ocurrio
  }else{
    digitalWrite(LEDPin,LOW);//apaga el led
    BT1.println(3);//envia por bluetooth la accion que ocurrio
  }
  
  //PIR
  int value= digitalRead(PIRPin); 
  if (value == HIGH and tiltActivado==0){//si hay movimiento se activa
    digitalWrite(LEDPin, HIGH);//enciende el led
    BT1.println(4);//envia por bluetooth la accion que ocurrio
  }else{
    digitalWrite(LEDPin, LOW);//apaga el led
    BT1.println(5);//envia por bluetooth la accion que ocurrio
  }

  //sensor de sonido
  volumen = analogRead(sonidoPin); //Se ha conectado el sensor a la placa por medio de la entrada A0
  if (volumen > rangoSonido){
    digitalWrite(LEDPin, HIGH) ;//enciende el led
    BT1.println(6);//envia por bluetooth la accion que ocurrio
    beep(200,10);//suena 10 veces, simula un tema
  }else {
    digitalWrite(LEDPin,LOW);//apaga el led  
    BT1.println(7);//envia por bluetooth la accion que ocurrio
  }

  //lectura del sensor TILT  
  lecturaTilt = digitalRead(TiltPin);//lee el estado del sensor
  if (lecturaTilt==LOW){
    digitalWrite(LEDPin, HIGH);//enciende el led
    BT1.println(8);//envia por bluetooth la accion que ocurrio
    tiltActivado=1;
  }else{
    digitalWrite(LEDPin, LOW);//apaga el led
    BT1.println(9);//envia por bluetooth la accion que ocurrio
    tiltActivado=0;
  }

  //cuando se apaga el sistema
  }else{//si se apaga el sistema, apaga todo lo que este encendido
    digitalWrite(LEDEncendido, LOW);//se apaga el led de encendido
    digitalWrite(LEDPin,LOW);//apaga el led de alerta
    tiltActivado=0;
  }

  delay(300);
  
}

//funcion que recibe el delay y la cantidad de veces a sonar
void beep(unsigned char pausa,int cantidad)
{
  int cant=0;
  while (cant <cantidad){   //loop
  analogWrite(buzzer, 255);  //enciende el buzzer
  delay(pausa);             // Espera un tiempo
  analogWrite(buzzer, 0);   // Apaga el buzzer
  delay(pausa);             // Espera un tiempo
  cant++;                   //incrementa la cantidad
  }
}

//funcion que se encarga de calcular el rango del sensor de sonido, para que se active al superarlo
void sonido()
{
  int cant=0; //cantidad de veces a repetir la muestra
  while(cant < 10){
    rangoSonido=analogRead(sonidoPin);  //obtiene el valor del sensor de sonido
    cant++; //incrementa el contador
  }
  rangoSonido=rangoSonido+((rangoSonido*20)/100);   //asigna el valor del rango
}
