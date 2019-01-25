package cl.coordinador.peajes;

/**
 * @(#)calc.java
 *
 * Se implementan distintas funciones para cálculos
 *
 * @author 
 * @version 1.00 2007/5/16
 */
import java.io.*;
import java.text.DecimalFormat;

public class Calc {
private static String slash=File.separator;
    //----------------------------------
    //Calcula matriz de Admitancia en pu base 100MVA, de acuerdo a datos de lineas
    //----------------------------------
    
    static public float[][] CalculaYBarra(float datosLineas[][], int numeroBarras, int numlineas) {
    	
    	float[][] YBarra=new float[numeroBarras][numeroBarras];
    	float Z=0;
    	float X=0;
    	float R=0;
    	boolean Operativa=false;
    	int barraA=0;
    	int barraB=0;
    	
    	for(int i=0;i<numeroBarras-1;i++){
            for(int j=0;j<numeroBarras-1;j++){
                YBarra[i][j]=0;
            }
    	}
    	
    	for(int l=0;l<numlineas;l++){
    		
            Operativa=(datosLineas[l][5]==1.0? true:false);

            if(Operativa==true){

                barraA=(int)datosLineas[l][0];
                barraB=(int)datosLineas[l][1];
                R=datosLineas[l][3];
                X=datosLineas[l][4];
                Z= (X*X+R*R)/X;
//              
                YBarra[barraA][barraB]+=1/Z;
                YBarra[barraB][barraA]+=1/Z;
                YBarra[barraA][barraA]-=1/Z;
                YBarra[barraB][barraB]-=1/Z;
            }
    	}
        return YBarra;
    }
    
    //----------------------------------
    //Determina Factores A para Calculo de GGDF    
    //----------------------------------
    
    static public float[][] CalculaGSDF(Matriz XBarra, float datosLineas[][], boolean barrasActivas[][],int e){
    	int numLineas=datosLineas.length;
    	int numBarras=XBarra.numeroFil();
    	float Aab=0;
    	float Xag=0;
    	float Xbg=0;
    	float Xab=0;
    	int barraA;
    	int barraB;
    	
    	float[][] A=new float [numBarras][numLineas];
    	for(int i=0;i<numBarras;i++){
            for(int j=0;j<numLineas;j++){
                A[i][j]=0;
            }
    	}
    	
    	for(int l=0;l<numLineas;l++){
            if((int)datosLineas[l][5]==1){ //si linea activa
                for(int b=0;b<numBarras;b++){
                    if(barrasActivas[b][e]==true){ //si la barra está activa
                        barraA=(int)datosLineas[l][0];
                        barraB=(int)datosLineas[l][1];
                        Xag=XBarra.get(barraA,b);
                        Xbg=XBarra.get(barraB,b);
                        Xab=datosLineas[l][4];
                        Aab=(Xag-Xbg)/Xab;
                        A[b][l]=(float)Aab;
                    }
                }
            }
    	}
    	return A;
    }
    
    //----------------------------------
    //Determina GGDF de barra de referencia
    //----------------------------------
    
    static public float[][] CalculaGGDFRef(float A[][], float Consumos[][],float datosLineas[][]){
    	int numBarras=Consumos.length;
    	int numLineas=datosLineas.length;
    	int numHid=Consumos[0].length;
    	float factorEscala=0;
    	float[][] DRef=new float[numLineas][numHid];
        float[] ConsumoSistema=new float[numHid];
    	float[][] ConsumoNormalizado=new float[numBarras][numHid];
    	
    	for(int h=0;h<numHid;h++){
            for(int l=0;l<numLineas;l++){
                DRef[l][h]=0;
            }
            for(int b=0;b<numBarras;b++){
                ConsumoSistema[h]+=Consumos[b][h];
            }
    	}
    	//System.out.println("Consumo sistema "+ConsumoSistema[1]);
        for(int h=0;h<numHid;h++){
            factorEscala=1/ConsumoSistema[h];
            for(int b=0;b<numBarras;b++){
                ConsumoNormalizado[b][h]=Consumos[b][h]*factorEscala;
            }
        }		
   	    	
    	for(int l=0;l<numLineas;l++){
            if((int)datosLineas[l][5]==1){
                for(int b=0;b<numBarras;b++){
                    for(int h=0;h<numHid;h++){
                        DRef[l][h]+=(float)(-A[b][l]*ConsumoNormalizado[b][h]);
                    }
                }
            }
    	}
    	return DRef;
    }
    
    //----------------------------------
    //Determina GGDF resto del sistema
    //----------------------------------
    
    static public float[][][] CalculaGGDF(float A[][], float Dref[][], float datosLineas[][], int e){
    	int numBarras=A.length;
    	int numLineas=datosLineas.length;
    	int numHid=Dref[0].length;
    	
    	float[][][] D=new float[numBarras][numLineas][numHid];
    	
    	for(int b=0;b<numBarras;b++){
            for(int l=0;l<numLineas;l++){
                for(int h=0;h<numHid;h++){
                    D[b][l][h]=0;
                }
            }
    	}
    	//System.out.println("REF Gx "+ Dref[603][1]);
        //System.out.println("A Gx"+ A[31][603]);
    	for(int b=0;b<numBarras;b++){
            for(int l=0;l<numLineas;l++){
                for(int h=0;h<numHid;h++){
                    if((int)datosLineas[l][5]==1){
                        D[b][l][h]=(A[b][l]+Dref[l][h]);
                    }
                }
            }
    	}
    	return D;
    }
      
    //----------------------------------
    //Determina Flujo de Potencia en función de GLDF, sin perdidas para una sola hidrologia
    //----------------------------------
    
    static public float[] FlujoDC_GLDF(float E[][][], float Consumos[][], int h, int e){
    	int numLineas=E[0].length;
    	int numBarras=E.length;
    	float[] Flujos=new float[numLineas];

        for(int l=0;l<numLineas;l++){
            Flujos[l]=0;
        }	
            for(int b=0;b<numBarras;b++){
                //System.out.println(b+" GLDF "+E[b][587][1]+" Consumo "+Consumos[b][1]);
                for(int l=0;l<numLineas;l++){
                Flujos[l]+=(float)(E[b][l][h]*Consumos[b][h]);

            }    		
    	}
    	return Flujos;
    }  
        
    //----------------------------------
    //Determina Prorratas de Generación
    //----------------------------------
    
    static public float[][] CalculaProrrGx(float flujoDC[][], float D[][][], float Gx[][][],int datosGener[][],
    						float datosLineas[][],int paramBarTroncal[][],int orientBarTroncal[][],int e, int[] gflux,int[] lineasFlujo, String DirBaseSalida,float A[][], float Dref[][]){
    	int numHid=flujoDC[0].length;
    	int numLineas=flujoDC.length;
    	int numGeneradores=Gx.length;
    	int barraGx=0;
    	int areaTroncal=0;
    	int sentidoFlujoLinea;
    	
        
        float[][] Prorratas=new float[numLineas][numGeneradores];
    	float[][] genEquivTotal=new float[numLineas][numHid];
    	float[][][] genEquiv=new float[numGeneradores][numLineas][numHid];

    	for(int i=0;i<numGeneradores;i++){
            for(int j=0;j<numLineas;j++){
                Prorratas[j][i]=0;
                for(int k=0;k<numHid;k++){
                    genEquiv[i][j][k]=0;
                }
            }
    	}

    	for(int l=0;l<numLineas;l++){
            for(int h=0;h<numHid;h++){
                genEquivTotal[l][h]=0;
            }
    	}

    	for(int h=0;h<numHid;h++){
            for(int l=0;l<numLineas;l++){
                if((int)datosLineas[l][5]==1){				// si la linea en servicio
                    if((int)datosLineas[l][6]==1){			// si la linea es troncal
                        if(flujoDC[l][h]!=0){
                            areaTroncal=(int) datosLineas[l][7];		// AIC=>0, Norte=>1, Sur=>-1
                            sentidoFlujoLinea=(int) datosLineas[l][8]; 	// 1 si linea apunta (nombre) al AIC, -1 en caso contrario.
                            for(int g=0;g<numGeneradores;g++){
                                barraGx=datosGener[g][0];
                                // si la barra de generacion está en la misma area troncal que la linea, o la línea está en el AIC
                                if(paramBarTroncal[barraGx][1]==areaTroncal || areaTroncal==0){
                                    // si está fuera del AIC y el flujo apunta a ella  y la barra está en el extremo contrario al AIC de la linea
                                    if(areaTroncal!=0 && (flujoDC[l][h]*sentidoFlujoLinea)>0 && orientBarTroncal[barraGx][l]==areaTroncal){
                                        if(Math.signum(flujoDC[l][h])==Math.signum(D[barraGx][l][h])){	//si esta aguas arriba del flujo
                                            genEquiv[g][l][h]=(float)(Gx[g][e][h]*Math.abs(D[barraGx][l][h]));
                                            //System.out.println("Prorratas "+Gx[g][e][h]);
                                            genEquivTotal[l][h]+=genEquiv[g][l][h];
                                        }
                                    }
                                    // si está en AIC y flujo tiene = signo que GGDF
                                    else if(areaTroncal==0 && (flujoDC[l][h]*D[barraGx][l][h])>0){
                                        genEquiv[g][l][h]=(float)(Gx[g][e][h]*D[barraGx][l][h]);
                                        genEquivTotal[l][h]+=genEquiv[g][l][h];
                                    }
                                }
                            }
                        }
                    }
                }
            }
    	}

        
        
        
        
    	for(int h=0;h<numHid;h++){
            for(int l=0;l<numLineas;l++){
                for(int g=0;g<numGeneradores;g++){
                    if((int) datosLineas[l][6]==1 && datosLineas[l][5]==1){
                        if(genEquivTotal[l][h]!=0){
                            Prorratas[l][g]+=(genEquiv[g][l][h]/genEquivTotal[l][h])/(float)numHid;
                        }
                    }
                }
            }
    	}
        
        
        try
	{
            FileWriter writer = new FileWriter(DirBaseSalida + slash +"prorratas.csv", true);
           
                        
            
            for(int h=0;h<numHid;h++){
                for(int l=0;l<lineasFlujo.length;l++){
                    for(int g=0;g<gflux.length;g++){
                    //for(int g=0;g<numGeneradores;g++){
                    
                        writer.append(String.valueOf(e));
                        writer.append(',');
                        writer.append(String.valueOf(h));
                        writer.append(',');
                        writer.append(String.valueOf(lineasFlujo[l]));
                        writer.append(',');
                        writer.append(String.valueOf(gflux[g]));
                        writer.append(',');
                        writer.append(String.valueOf((genEquiv[gflux[g]][lineasFlujo[l]][h]) /(genEquivTotal[lineasFlujo[l]][h])));
                        writer.append(',');
                        writer.append(""+ (Gx[gflux[g]][e][h]));
                        writer.append(',');
                        writer.append(""+ (D[datosGener[gflux[g]][0]][lineasFlujo[l]][h]));
                        writer.append(',');
                        writer.append(""+ (A[datosGener[gflux[g]][0]][lineasFlujo[l]]));
                        writer.append(',');
                        writer.append(""+ (Dref[lineasFlujo[l]][h]));
                        
                        
                                
                                
                        writer.append('\n');

                    }
                }
               
            }
            
            for(int l=0;l<lineasFlujo.length;l++){
                    for(int g=0;g<gflux.length;g++){
                    //for(int g=0;g<numGeneradores;g++){
                        writer.append(String.valueOf(e));
                        writer.append(',');
                        writer.append("med");
                        writer.append(',');
                        writer.append(String.valueOf(lineasFlujo[l]));
                        writer.append(',');
                        writer.append(String.valueOf(gflux[g]));
                        writer.append(',');
                        writer.append(""+(Prorratas[lineasFlujo[l]][gflux[g]]));
                        //System.out.println(Prorratas[l][gflux[g]]);
                        writer.append('\n');

                    }
                }
            
            writer.flush();
            writer.close();
        }
        
        catch(IOException f)
	{
	     f.printStackTrace();
	} 
        
        
        
    	return Prorratas;
    }
    
    //----------------------------------
    //Determina GLDF de barra de referencia
    //----------------------------------
    
    static public float[][] CalculaGLDFRef(float A[][], float datosLineas[][], int datosGener[][],int e,float Generacion[][][]){
    	int numBarras=A.length;
    	int numLineas=datosLineas.length;
    	int numHid=Generacion[0][0].length;
    	int numGen=Generacion.length;
    	float[][] ERef=new float[numLineas][numHid];
    	float[][] GxBarra=new float[numBarras][numHid];
    	float[] GxTotal=new float[numHid];

    	for(int h=0;h<numHid;h++){
            GxTotal[h]=0;
            for(int b=0;b<numBarras;b++){
                GxBarra[b][h]=0;
            }
            for(int l=0;l<numLineas;l++){
                ERef[l][h]=0;
            }
    	}

    	for(int g=0;g<numGen;g++){
            for(int h=0;h<numHid;h++){
                GxTotal[h]+=Generacion[g][e][h];
                GxBarra[datosGener[g][0]][h]+=Generacion[g][e][h];
            }
        }

    	for(int l=0;l<numLineas;l++){
            if((int)datosLineas[l][5]==1){
                for(int b=0;b<numBarras;b++){
                    for(int h=0;h<numHid;h++){
                        ERef[l][h]-=(float)(A[b][l]*GxBarra[b][h]/GxTotal[h]);
                    }
                }
            }
    	}
    	return ERef;
    }
    
    //----------------------------------
    //Determina GLDF resto del sistema
    //----------------------------------
    
    static public float[][][] CalculaGLDF(float A[][], float Eref[][], float datosLineas[][],int e){
    	int numBarras=A.length;
    	int numLineas=datosLineas.length;
    	int numHid=Eref[0].length;
    	
    	float[][][] E=new float[numBarras][numLineas][numHid];
    	
    	for(int b=0;b<numBarras;b++){
            for(int l=0;l<numLineas;l++){
                for(int h=0;h<numHid;h++){
                    E[b][l][h]=0;
                }
            }
    	}
        //System.out.println("REF Gx "+ Eref[586][1]);
        //System.out.println("A Gx"+ A[31][586]);
    	
    	for(int b=0;b<numBarras;b++){
            for(int l=0;l<numLineas;l++){
                if((int)datosLineas[l][5]==1){
                    for(int h=0;h<numHid;h++){
                        E[b][l][h]=-(A[b][l]+Eref[l][h]);
                    }
                }
            }
    	}
    	return E;
    }
     
    
    //----------------------------------
    //Determina asignacion de perdidas en las lineas
    //----------------------------------
    
    static public float[] ProrrPerdidas(float PerdidaAProrratear, float [] perdidasI2R, float datosLineas[][],
                                            String tipo, int h) {
        int numLineas=perdidasI2R.length;
        float perdidaTotalI2R=0;
        float[] perdidas=new float[numLineas];
        for(int l=0;l<numLineas;l++){
            if(tipo.equals("Mayor_110")) {
                if(datosLineas[l][2]>110) {
                    perdidaTotalI2R+=perdidasI2R[l];
                }
            }
            else{
                if(datosLineas[l][2]<=110){
                    perdidaTotalI2R+=perdidasI2R[l];
                }
            }
        }
        //System.out.println("Perdidas PLP "+PerdidaAProrratear);
        //System.out.println("Perdidas PLP "+perdidaTotalI2R);
        for(int l=0;l<numLineas;l++){
            if(tipo.equals("Mayor_110")) {
                if(datosLineas[l][2]>110) {
                    perdidas[l]=PerdidaAProrratear*perdidasI2R[l]/perdidaTotalI2R;
                }   
            }
            else{
                if(datosLineas[l][2]<=110) {
                    perdidas[l]=PerdidaAProrratear*perdidasI2R[l]/perdidaTotalI2R;
                }
            }            
        }
        return perdidas;
    }
        
    //----------------------------------
    //Determina asignacion de perdidas a los consumos, para 1 hidrologia
    //----------------------------------
    
    static public float[] AsignaPerdidas(float flujoDC[], float E[][][], float perdidas[], float datosLineas[][],
    										float Consumos[][], int e, int h){
    	int numBarras=Consumos.length;
    	int numLineas=flujoDC.length;
    	float[] consumoModif=new float[numBarras];    	
    	
    	float[] ConsEquivTotal=new float[numLineas];
    	float[][] ConsEquiv=new float[numBarras][numLineas];
    	
        for(int l=0;l<numLineas;l++){
            ConsEquivTotal[l]=0;
        }    		
        for(int b=0;b<numBarras;b++){
            consumoModif[b]=Consumos[b][h];
            for(int l=0;l<numLineas;l++){
                ConsEquiv[b][l]=0;
            }
        }    	

        for(int l=0;l<numLineas;l++){
            if((int)datosLineas[l][5]==1){						// si la linea está en servicio
                if(flujoDC[l]!=0){
                    for(int b=0;b<numBarras;b++){
                        if(Math.signum(flujoDC[l])==Math.signum(E[b][l][h])){           //si está aguas abajo del flujo
                            ConsEquiv[b][l]=(float)(Math.abs(E[b][l][h]))*Consumos[b][h];
                            ConsEquivTotal[l]+=ConsEquiv[b][l];
                        }
                    }
                }
            }
        }
    	
        for(int l=0;l<numLineas;l++){
            for(int b=0;b<numBarras;b++){
                if(datosLineas[l][5]==1){
                    if(ConsEquivTotal[l]!=0){
                        consumoModif[b]+=(ConsEquiv[b][l]/ConsEquivTotal[l])*perdidas[l];
                        //System.out.println("Consumo modificado "+consumoModif[b]);
                    }
                }
            }
        }    	
    	return consumoModif;
    } 
    
    //----------------------------------
    //Determina Prorratas de Consumos
    //----------------------------------
    
    public static void scalarMultiply(double[][] array, double scale){
        for( int i=0; i<array.length; i++){
            for( int j=0; j<array[i].length; j++){
                array[i][j] = array[i][j] * scale; 
            }
        }
   }
    
   public static void scalarMultiply(int[][] array, int scale){
        for( int i=0; i<array.length; i++){
            for( int j=0; j<array[i].length; j++){
                array[i][j] = array[i][j] * scale; 
            }
        }
   }
    
        static public float[][] CalculaProrrCons(float flujoDC[][], float E[][][], float Consumos[][],int datosClientes[][],
    						float datosLineas[][], int paramBarTroncal[][],int orientBarTroncal[][],int e){
    	int numHid=flujoDC[0].length;
    	int numLineas=flujoDC.length;
    	int numClientes=Consumos.length;
    	int areaTroncal=0;
    	int sentidoFlujoLinea;
    	int barraCx=0;
        int barraCx2=0;
        int sicosing=0;
    	float[][] Prorratas=new float[numLineas][numClientes];
    	float[][] ConsEquivTotal=new float[numLineas][numHid];
    	float[][][] ConsEquiv=new float[numClientes][numLineas][numHid];
    	
    	for(int i=0;i<numClientes;i++){
            for(int j=0;j<numLineas;j++){
                Prorratas[j][i]=0;
                for(int k=0;k<numHid;k++){
                    ConsEquiv[i][j][k]=0;
                }
            }
    	}
   	
    	for(int l=0;l<numLineas;l++){
            for(int h=0;h<numHid;h++){
                ConsEquivTotal[l][h]=0;
            }
    	}
    	
    	for(int h=0;h<numHid;h++){
            for(int l=0;l<numLineas;l++){
                if((int)datosLineas[l][5]==1){						// si la linea está en servicio
                    if((int) datosLineas[l][6]==1){					// si la línea es troncal
                        if(flujoDC[l][h]!=0){
                            areaTroncal=(int) datosLineas[l][7];			// AIC=>0, Norte=>1, Sur=>-1
                            sentidoFlujoLinea=(int) datosLineas[l][8]; 	// 1 si linea apunta (nombre) al AIC, -1 en caso contrario.
                            sicosing=(int) datosLineas[l][9];
                            for(int c=0;c<numClientes;c++){
                                barraCx=datosClientes[c][0];
                                //System.out.println(c+" "+barraCx);
                                //System.out.println(c+" "+barraCx+" "+paramBarTroncal[barraCx][1]+" "+areaTroncal);
                                //if (paramBarTroncal[barraCx][2]==sicosing && datosClientes[c][3]==1){
                                //if (datosClientes[c][3]==0 || (datosClientes[c][3]==1 && paramBarTroncal[barraCx][2]==sicosing)){
                                    //barraCx2=datosClientes[c][3];
                                    //System.out.println(c+" "+barraCx2);
                                    //barraCx2=paramBarTroncal[barraCx][2];
                                    //System.out.println(c+" "+barraCx2+" "+barraCx);
                                    //System.out.println(" ");
                                    //barraCx2=sicosing;
                                    //System.out.println(c+" "+barraCx2);

                                    // si la barra de consumo está en la misma area troncal que la linea o en el AIC
                                    if(paramBarTroncal[barraCx][1]==areaTroncal || areaTroncal==0){
                                         // si está fuera del AIC y flujo alejandose y la barra está en el extremo contrario al AIC de la linea
                                        if(areaTroncal!=0 && flujoDC[l][h]*sentidoFlujoLinea<0 && orientBarTroncal[barraCx][l]==areaTroncal){
                                            if(Math.signum(flujoDC[l][h])==Math.signum(E[barraCx][l][h])){	//si está aguas abajo del flujo
                                            ConsEquiv[c][l][h]=(float)(Math.abs(E[barraCx][l][h]))*Consumos[c][e];
                                            ConsEquivTotal[l][h]+=ConsEquiv[c][l][h];
                                             }
                                        }
                                         // si está en AIC y flujo tiene = signo que GLDF
                                        else if(areaTroncal==0 && (flujoDC[l][h]*E[barraCx][l][h])>0){
                                        ConsEquiv[c][l][h]=(float)(Math.abs(E[barraCx][l][h]))*Consumos[c][e];
                                        ConsEquivTotal[l][h]+=ConsEquiv[c][l][h];
                                        }
                                    }
                                
                            }
                        }
                    }
                }
            }
    	}
    	
    	for(int h=0;h<numHid;h++){
            for(int l=0;l<numLineas;l++){
                for(int c=0;c<numClientes;c++){
                    //System.out.println(h+" "+l+" "+c);
                    barraCx=datosClientes[c][0];
                    if((int) datosLineas[l][6]==1 && datosLineas[l][5]==1){
                        if(ConsEquivTotal[l][h]!=0){
                            
                            if(datosClientes[c][3]==1 && paramBarTroncal[barraCx][2]!=datosLineas[l][9]){
                            
                            Prorratas[l][c]=0;
                            //System.out.println(" ");
                            //System.out.println(barraCx+" "+l);
                            //System.out.println(Prorratas[l][c]);
                            //System.out.println(datosLineas[l][9]+ " "+ paramBarTroncal[barraCx][2]);
                            }
                            else {
                            
                            Prorratas[l][c]+=(ConsEquiv[c][l][h]/ConsEquivTotal[l][h])/(float)numHid;   
                                
                          //if(l==1370 && c==649){
                            
                         //System.out.println(Prorratas[l][c]);      
                          //}
                        //System.out.println(Prorratas[l][c]);
                                }
                            }
                        }
                 
                    }                
                }
            }
            return Prorratas;
        }
    
    
    //----------------------------------
    //----------------------------------
    // FUNCIONES DE UTILIDAD GENERAL
    //----------------------------------
    //----------------------------------    
        
    //----------------------------------
    //Determina el valor máximo en un arreglo
    //----------------------------------
   static public float Maximo(float Arreglo[]){
    	int dim=Arreglo.length;
    	float maximo=0;
    	for(int i=0;i<dim;i++){
            maximo=Math.max(maximo,Arreglo[i]);
    	}
    	return maximo;
    }
    
    //----------------------------------
    //Busqueda en arreglos de texto
    //----------------------------------
    static public int Buscar(String Buscado,String Datos[]){
    	int largo=Datos.length;
    	int Encontrado=-1;
    	for(int i=0;i<largo;i++){
            if (Datos[i].equals(Buscado)){
                Encontrado=i;
            }
    	}
    	return Encontrado;
    }
    
    //----------------------------------
    //Elimina filas en arreglos  
    //----------------------------------
    static public float[][] EliminarFilaArr(float Original[][], int fila, int nfilas, int ncolumnas){
        float[][] X=new float[nfilas-1][ncolumnas];
        for(int i=0;i<nfilas;i++){
            for(int j=0;j<ncolumnas;j++){
                if(i<fila)
                    X[i][j]=Original[i][j];
                else if(i>fila)
                    X[i-1][j]=Original[i][j];
            }
        }
        return X;
    }

    //----------------------------------
    //Elimina columnas en arreglos  
    //----------------------------------
    static public float[][] EliminarColumnaArr(float[][] Original, int col, int nfilas, int ncolumnas){
        float[][] X=new float[nfilas][ncolumnas-1];
        for(int i=0;i<nfilas;i++){
            for(int j=0;j<ncolumnas;j++){
                if(j<col)
                    X[i][j]=Original[i][j];
                else if (j>col)
                    X[i][j-1]=Original[i][j];
            }
        }
        return X;
    }

    static public double[][] transponer(double [][] Original){
        int m=Original.length;
        int n=Original[0].length;
        double[][] Transpuesta=new double[n][m];
        for(int i=0;i<m;i++){
            for(int j=0;j<n;j++){
                Transpuesta[j][i]=Original[i][j];
            }
        }
        return Transpuesta;
    }

    //----------------------------------
    //Determina si quedan barras "sueltas"
    //----------------------------------
    static public boolean[][] ChequeoConsistencia(float datosLineas[][], int mantLineas[][], int numBarras, int numEtapas){
   		
            boolean[][] barrasActivas=new boolean[numBarras][numEtapas];
            int [][] barrasConectadas=new int[numBarras][numEtapas];
            boolean operativa=false;
            int numLineas=datosLineas.length;
            int barraA=0;
            int barraB=0;
   		
            for(int b=0;b<numBarras;b++){
                    for(int e=0;e<numEtapas;e++){
                            barrasConectadas[b][e]=0;
                    }
            }

            for(int e=0;e<numEtapas;e++){
                for(int l=0;l<numLineas;l++){
                    operativa=((int)datosLineas[l][5]==1? true:false);
                    if((int)mantLineas[l][e]!=(-1)){
                        operativa=((int)mantLineas[l][e]==1? true:false);
                    }
                    if(operativa==true){//si linea activa
                        barraA=((int)datosLineas[l][0]);
                        barraB=((int)datosLineas[l][1]);
                        barrasConectadas[barraA][e]++;
                        barrasConectadas[barraB][e]++;
                    }
                }
                for(int b=0;b<numBarras;b++){
                    barrasActivas[b][e]=(barrasConectadas[b][e]>0? true:false);
                }
            }
            return barrasActivas;
   	}

   public static int[] OrdenarBurbujaStr(String[] texto){
       int temp;
       int t = texto.length;
       int[] n = new int[t];
       for (int i = 1; i < t; i++)
           n[i] = i;
       for (int i = 1; i < t; i++) {
           for (int k = t- 1; k >= i; k--) {
               if(texto[n[k]].compareTo(texto[n[k-1]]) < 0){
                   temp = n[k];
                   n[k] = n[k-1];
                   n[k-1]=  temp;
               }
           }
       }
       return n;
   }

   public static int[] OrdenarBurbujaInt(int[] num){
       int temp;
       int t = num.length;
       int[] n = new int[t];
       for (int i = 1; i < t; i++)
           n[i] = i;
       for (int i = 1; i < t; i++) {
           for (int k = t- 1; k >= i; k--) {
               if(num[n[k]] < num[n[k-1]]){
                   temp = n[k];
                   n[k] = n[k-1];
                   n[k-1]=  temp;
               }
           }
       }
       return n;
   }

}