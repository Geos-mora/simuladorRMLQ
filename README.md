# Simulador de Planificación Multinivel (MLQ)

Este proyecto implementa un **simulador de colas multinivel (Multi-Level Queue Scheduling)** en **Java**, desarrollado para el curso de sistemas operativos / programación concurrente (o similar).  
Permite simular diferentes políticas de planificación: **RR**, **FCFS**, **SJF** y **STCF**, con entradas y salidas definidas por archivos de texto.

---

## Estructura del proyecto
SIMULADORMLQ/
├── data/
│ ├── entradas/
│ │ ├── mlq001.txt
│ │ ├── mlq002.txt
│ │ ├── mlq003.txt
│ │ ├── mlq004.txt
│ │ └── mlq005.txt
│ └── salidas/
│ ├── salida_mlq001.txt
│ ├── salida_mlq002.txt
│ ├── salida_mlq003.txt
│ ├── salida_mlq004.txt
│ └── salida_mlq005.txt
├── src/
│ └── MainMLQ.java
└── .vscode/
└── launch.json


---

## Requisitos ##

Antes de ejecutar el proyecto, asegúrate de tener instalado:

- **Java JDK 17** o superior  
- **Visual Studio Code**  
- **Extension Pack for Java** (contiene el depurador, lenguaje y herramientas necesarias) se lo instala en la store de extensiones 

---

## Ejecución en Visual Studio Code  ##

1. **Clonar el repositorio:**
   git clone https://github.com/Geos-mora/simuladorRMLQ.git
   cd SIMULADORMLQ

2. Abrir la carpeta en VS Code
3. Verificar que las extensiones Java estén instaladas:

Language Support for Java (by Red Hat)

    Debugger for Java
    Project Manager for Java

4. Ejecutar el proyecto

       Abre la vista Run and Debug (Ctrl + Shift + D)
       Selecciona la configuración Run SIMULADORMLQ
       Presiona F5 o el botón

El simulador usará la configuración de ejecución definida en .vscode/launch.json, por ejemplo:

  data/entradas/mlq001.txt data/salidas/salida_mlq001.txt --scheme 1 --log

