import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MainMLQ {

        /* ------------Tiipos auxiliares ------------*/
        enum Politica { RR, FCFS, SJF, STCF }

        static class TramoEjecucion {
            final String etiqueta;
            final int inicio;
            final int duracion;
            final int cola;

            TramoEjecucion(String etiqueta, int inicio, int duracion, int cola) {
                this.etiqueta= etiqueta;
                this.inicio= inicio;
                this.duracion= duracion;
                this.cola= cola;
            }
        }


        static class Proceso {
            String etiqueta;
            int bt, at, cola, prioridad;
            int restante;
            Integer primerInicio= null;     /* primer instante en que corre*/
            Integer fin= null;              /* completion time*/
            Integer respuesta= null;        /* response time= primerInicio - at*/
            Integer espera= null;           /* waiting time*/
            Integer retorno= null;          /* turnaround time= fin - at*/

            Proceso(String etiqueta, int bt, int at, int cola, int prioridad) {
                this.etiqueta= etiqueta;
                this.bt= bt;
                this.at= at;
                this.cola= cola;
                this.prioridad= prioridad;
                this.restante= bt;
            }

            @Override
            public String toString() {
                return String.format("%s(BT=%d,AT=%d,Q=%d,Pr=%d,rem=%d)",
                        etiqueta, bt, at, cola, prioridad, restante);
            }
        }


        static class PlanificadorMLQ {
            int tiempo= 0;

            /* Lista completa de procesos (ordenada por llegada, luego -prioridad)*/
            final List<Proceso> todos;
            /* Procesos que aún no han llegado (ordenados)*/
            final LinkedList<Proceso> noLlegados;

            /* Tres colas (1..3). Usamos LinkedList como deque.*/
            final Map<Integer, Deque<Proceso>> colas= new HashMap<>();

            /* Políticas por cola: (politica, parámetro). Para RR, parámetro=quantum; otro= null.*/
            final Map<Integer, AbstractMap.SimpleEntry<Politica, Integer>> politicas= new HashMap<>();

            /* Terminados y log de ejecución*/
            final List<Proceso> terminados= new ArrayList<>();
            final List<TramoEjecucion> logEjecucion= new ArrayList<>();

            PlanificadorMLQ(List<Proceso> procesos, int esquema) {
                /* ordenar por (AT asc, prioridad desc) estable*/
                procesos.sort(Comparator
                        .comparingInt((Proceso p) -> p.at)
                        .thenComparing((Proceso p) -> -p.prioridad));

                this.todos= new ArrayList<>(procesos);
                this.noLlegados= new LinkedList<>(procesos);

                colas.put(1, new LinkedList<>());
                colas.put(2, new LinkedList<>());
                colas.put(3, new LinkedList<>());

                if (esquema==1) {
                    politicas.put(1, new AbstractMap.SimpleEntry<>(Politica.RR, 1));
                    politicas.put(2, new AbstractMap.SimpleEntry<>(Politica.RR, 3));
                    politicas.put(3, new AbstractMap.SimpleEntry<>(Politica.SJF, null));
                } else if (esquema==2) {
                    politicas.put(1, new AbstractMap.SimpleEntry<>(Politica.RR, 3));
                    politicas.put(2, new AbstractMap.SimpleEntry<>(Politica.RR, 5));
                    politicas.put(3, new AbstractMap.SimpleEntry<>(Politica.FCFS, null));
                } else if (esquema==3) {
                    politicas.put(1, new AbstractMap.SimpleEntry<>(Politica.RR, 2));
                    politicas.put(2, new AbstractMap.SimpleEntry<>(Politica.RR, 3));
                    politicas.put(3, new AbstractMap.SimpleEntry<>(Politica.STCF, null));
                } else {
                    throw new IllegalArgumentException("scheme debe ser 1, 2 o 3");
                }
            }

            /** Encolar procesos cuya llegada <= tiempo actual. */
            void encolarLlegadas() {
                while (!noLlegados.isEmpty() && noLlegados.peekFirst().at <= tiempo) {
                    Proceso p= noLlegados.removeFirst();
                    encolar(p);
                }
            }

            /** Inserta el proceso p en su cola respetando la política de ordenamiento. */
            void encolar(Proceso p) {
                int q= p.cola;
                Politica pol= politicas.get(q).getKey();
                Deque<Proceso> dq= colas.get(q);

                if (pol==Politica.SJF || pol==Politica.STCF) {
                    /* mantener orden por (restante asc), luego (AT asc), luego etiqueta lexicográfica*/
                    LinkedList<Proceso> tmp= new LinkedList<>();
                    boolean insertado= false;
                    while (!dq.isEmpty()) {
                        Proceso cur= dq.removeFirst();
                        if (!insertado && compararSJF(p, cur)<0) {
                            tmp.addLast(p);
                            insertado= true;
                        }
                        tmp.addLast(cur);
                    }
                    if (!insertado) tmp.addLast(p);
                    colas.put(q, tmp);
                } else {
                    /* RR y FCFS: al final*/
                    dq.addLast(p);
                }
            }

            /** Comparación para SJF/STCF: por restante, luego llegada, luego etiqueta. */
            int compararSJF(Proceso a, Proceso b) {
                if (a.restante != b.restante) return Integer.compare(a.restante, b.restante);
                if (a.at != b.at) return Integer.compare(a.at, b.at);
                return a.etiqueta.compareTo(b.etiqueta);
            }

            /** ¿Hay algún proceso listo en alguna cola? */
            boolean hayListos() {
                return !colas.get(1).isEmpty() || !colas.get(2).isEmpty() || !colas.get(3).isEmpty();
            }

            /** Siguiente tiempo de llegada a una cola de mayor prioridad que 'nivelColaActual'. */
            Integer proximaLlegadaColaSuperior(int nivelColaActual) {
                Integer mejor= null;
                for (Proceso p : noLlegados) {
                    if (p.cola<nivelColaActual) {
                        if (mejor==null || p.at<mejor) mejor= p.at;
                    }
                }
                return mejor;
            }

            /** Ejecuta la simulación completa. */
            void ejecutar() {
                /* Si no hay nada en t=0, saltar al primer arribo*/
                if (!noLlegados.isEmpty() && !hayListos()) {
                    tiempo= noLlegados.peekFirst().at;
                }

                while (terminados.size()<todos.size()) {
                    encolarLlegadas();

                    if (!hayListos()) {
                        /* No hay listos: saltar directo al próximo arribo*/
                        if (!noLlegados.isEmpty()) {
                            tiempo= noLlegados.peekFirst().at;
                            continue;
                        } else {
                            break;
                        }
                    }

                    /* Tomar la cola no vacía de mayor prioridad (1..3)*/
                    int qActual= (!colas.get(1).isEmpty()) ? 1 :
                            (!colas.get(2).isEmpty()) ? 2 : 3;

                    Politica pol= politicas.get(qActual).getKey();
                    Integer param= politicas.get(qActual).getValue();

                    if (pol==Politica.RR) {
                        Proceso proc= colas.get(qActual).removeFirst();
                        int quantum= param;
                        int correr= Math.min(quantum, proc.restante);

                        Integer proxSup= proximaLlegadaColaSuperior(qActual);
                        if (proxSup != null && proxSup<tiempo + correr) {
                            correr= Math.max(0, proxSup - tiempo);
                        }

                        if (correr==0) {
                            tiempo= proxSup; /* salto al evento que interrumpe*/
                            encolarLlegadas();
                            colas.get(qActual).addFirst(proc); /* no corrió*/
                            continue;
                        }

                        if (proc.primerInicio==null) {
                            proc.primerInicio= tiempo;
                            proc.respuesta= proc.primerInicio - proc.at;
                        }

                        logEjecucion.add(new TramoEjecucion(proc.etiqueta, tiempo, correr, qActual));
                        tiempo += correr;
                        proc.restante -= correr;

                        encolarLlegadas();

                        if (proc.restante>0) {
                            colas.get(qActual).addLast(proc);
                        } else {
                            proc.fin= tiempo;
                            terminados.add(proc);
                        }

                    } else if (pol==Politica.FCFS) {
                        Proceso proc= colas.get(qActual).removeFirst();
                        int correr= proc.restante;

                        Integer proxSup= proximaLlegadaColaSuperior(qActual);
                        if (proxSup != null && proxSup<tiempo + correr) {
                            correr= Math.max(0, proxSup - tiempo);
                        }

                        if (correr==0) {
                            tiempo= proxSup;
                            encolarLlegadas();
                            colas.get(qActual).addFirst(proc); /* preservar orden FCFS*/
                            continue;
                        }

                        if (proc.primerInicio==null) {
                            proc.primerInicio= tiempo;
                            proc.respuesta= proc.primerInicio - proc.at;
                        }

                        logEjecucion.add(new TramoEjecucion(proc.etiqueta, tiempo, correr, qActual));
                        tiempo += correr;
                        proc.restante -= correr;

                        encolarLlegadas();

                        if (proc.restante>0) {
                            colas.get(qActual).addFirst(proc); /* FCFS continúa primero*/
                        } else {
                            proc.fin= tiempo;
                            terminados.add(proc);
                        }

                    } else if (pol==Politica.SJF) {
                        Proceso proc= colas.get(qActual).removeFirst();
                        int correr= proc.restante;

                        Integer proxSup= proximaLlegadaColaSuperior(qActual);
                        if (proxSup != null && proxSup<tiempo + correr) {
                            correr= Math.max(0, proxSup - tiempo);
                        }

                        if (correr==0) {
                            tiempo= proxSup;
                            encolarLlegadas();
                            colas.get(qActual).addFirst(proc);
                            continue;
                        }

                        if (proc.primerInicio==null) {
                            proc.primerInicio= tiempo;
                            proc.respuesta= proc.primerInicio - proc.at;
                        }

                        logEjecucion.add(new TramoEjecucion(proc.etiqueta, tiempo, correr, qActual));
                        tiempo += correr;
                        proc.restante -= correr;

                        encolarLlegadas();

                        if (proc.restante>0) {
                            /* No debería ocurrir salvo preempción por cola superior*/
                            colas.get(qActual).addFirst(proc);
                        } else {
                            proc.fin= tiempo;
                            terminados.add(proc);
                        }

                    } else if (pol==Politica.STCF) {
                        /* SJF expropiativo dentro de la misma cola*/
                        Proceso proc= colas.get(qActual).removeFirst();

                        Integer proxSup= proximaLlegadaColaSuperior(qActual);
                        Integer proxMismaCola= null;
                        for (Proceso p : noLlegados) {
                            if (p.cola==qActual) {
                                proxMismaCola= p.at;
                                break;
                            }
                        }
                        Integer siguienteEvento= null;
                        if (proxSup != null && proxMismaCola != null) {
                            siguienteEvento= Math.min(proxSup, proxMismaCola);
                        } else if (proxSup != null) {
                            siguienteEvento= proxSup;
                        } else if (proxMismaCola != null) {
                            siguienteEvento= proxMismaCola;
                        }

                        int correr= proc.restante;
                        if (siguienteEvento != null && siguienteEvento<tiempo + correr) {
                            correr= Math.max(0, siguienteEvento - tiempo);
                        }

                        if (correr==0) {
                            tiempo= siguienteEvento;
                            encolarLlegadas();
                            colas.get(qActual).addFirst(proc);
                            continue;
                        }

                        if (proc.primerInicio==null) {
                            proc.primerInicio= tiempo;
                            proc.respuesta= proc.primerInicio - proc.at;
                        }

                        logEjecucion.add(new TramoEjecucion(proc.etiqueta, tiempo, correr, qActual));
                        tiempo += correr;
                        proc.restante -= correr;

                        encolarLlegadas();

                        if (proc.restante>0) {
                            /* Reinsertar manteniendo orden por restante (SJF)*/
                            encolar(proc);
                        } else {
                            proc.fin= tiempo;
                            terminados.add(proc);
                        }
                    } else {
                        throw new IllegalStateException("Política desconocida");
                    }
                }

                /* Calcular métricas*/
                for (Proceso p : todos) {
                    if (p.fin==null) p.fin= tiempo;
                    p.retorno= p.fin - p.at;
                    p.espera= p.retorno - p.bt;
                    if (p.respuesta==null) p.respuesta= 0; /* por seguridad*/
                }
            }

            /** Escribe la salida al archivo de texto (formato igual al Python). */
            void escribirSalida(String rutaSalida) throws IOException {
                try (BufferedWriter bw= new BufferedWriter(
                        new OutputStreamWriter(new FileOutputStream(rutaSalida), StandardCharsets.UTF_8))) {
                    bw.write("# etiqueta; BT; AT; Q; Pr; WT; CT; RT; TAT\n");
                    double sumWT= 0, sumCT= 0, sumRT= 0, sumTAT= 0;
                    int n= todos.size();

                    /* Orden estable por (AT, etiqueta)*/
                    List<Proceso> copia= new ArrayList<>(todos);
                    copia.sort(Comparator
                            .comparingInt((Proceso p) -> p.at)
                            .thenComparing(p -> p.etiqueta));

                    for (Proceso p : copia) {
                        int wt= (p.espera != null) ? p.espera : 0;
                        int ct= (p.fin != null) ? p.fin : tiempo;
                        int rt= (p.respuesta != null) ? p.respuesta : 0;
                        int tat= (p.retorno != null) ? p.retorno : (ct - p.at);

                        bw.write(String.format("%s;%d;%d;%d;%d;%d;%d;%d;%d\n",
                                p.etiqueta, p.bt, p.at, p.cola, p.prioridad, wt, ct, rt, tat));

                        sumWT += wt;
                        sumCT += ct;
                        sumRT += rt;
                        sumTAT += tat;
                    }
                    double avgWT= (n>0) ? sumWT / n : 0.0;
                    double avgCT= (n>0) ? sumCT / n : 0.0;
                    double avgRT= (n>0) ? sumRT / n : 0.0;
                    double avgTAT= (n>0) ? sumTAT / n : 0.0;

                    bw.write("\n");
                    bw.write(String.format("WT=%.2f; CT=%.2f; RT=%.2f; TAT=%.2f;\n",
                            avgWT, avgCT, avgRT, avgTAT));
                }
            }

            /** Imprime el log de ejecución (útil para Gantt). */
            void imprimirLog() {
                for (TramoEjecucion t : logEjecucion) {
                    int fin= t.inicio + t.duracion;
                    System.out.printf("%4d - %4d : %s (Q%d)%n", t.inicio, fin, t.etiqueta, t.cola);
                }
            }
        }

        /* ====== Utilidades de entrada/salida ======*/

        /** Parsea el archivo de entrada y construye la lista de procesos. */
        static List<Proceso> leerEntrada(String ruta) throws IOException {
            List<Proceso> lista= new ArrayList<>();
            try (BufferedReader br= new BufferedReader(new InputStreamReader(
                    new FileInputStream(ruta), StandardCharsets.UTF_8))) {
                String linea;
                while ((linea= br.readLine()) != null) {
                    linea= linea.trim();
                    if (linea.isEmpty() || linea.startsWith("#")) continue;

                    String[] partes= linea.split(";");
                    if (partes.length<5) continue;

                    String etiqueta= partes[0].trim();
                    int bt= Integer.parseInt(partes[1].trim());
                    int at= Integer.parseInt(partes[2].trim());
                    int q= Integer.parseInt(partes[3].trim());
                    int pr= Integer.parseInt(partes[4].trim());
                    lista.add(new Proceso(etiqueta, bt, at, q, pr));
                }
            }
            /* ordenar estable por (AT, -prioridad)*/
            lista.sort(Comparator
                    .comparingInt((Proceso p) -> p.at)
                    .thenComparing((Proceso p) -> -p.prioridad));
            return lista;
        }

        /** Parseo mínimo de argumentos estilo "--clave valor" y flags booleanos. */
        static class Args {
            final String input;
            final String output;
            final int scheme;
            final boolean log;

            Args(String[] argv) {
                if (argv.length<2) {
                    throw new IllegalArgumentException("Uso: java MainMLQ <input> <output> [--scheme 1|2|3] [--log]");
                }
                this.input= argv[0];
                this.output= argv[1];

                int _scheme= 1;
                boolean _log= false;
                for (int i= 2; i<argv.length; i++) {
                    String a= argv[i];
                    if ("--scheme".equals(a)) {
                        if (i + 1 >= argv.length) {
                            throw new IllegalArgumentException("Falta valor para --scheme");
                        }
                        _scheme= Integer.parseInt(argv[++i]);
                        if (_scheme<1 || _scheme>3) {
                            throw new IllegalArgumentException("--scheme debe ser 1, 2 o 3");
                        }
                    } else if ("--log".equals(a)) {
                        _log= true;
                    } else {
                        throw new IllegalArgumentException("Argumento no reconocido: " + a);
                    }
                }
                this.scheme= _scheme;
                this.log= _log;
            }
        }

        /* ====== Main ======*/
        public static void main(String[] argv) {
            try {
                Args args= new Args(argv);
                List<Proceso> procs= leerEntrada(args.input);
                PlanificadorMLQ plan= new PlanificadorMLQ(procs, args.scheme);
                plan.ejecutar();
                plan.escribirSalida(args.output);
                if (args.log) {
                    plan.imprimirLog();
                }
                System.out.println("Simulación completa. Resultados escritos en: " + args.output);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace(System.err);
                System.exit(1);
            }
        }
}


