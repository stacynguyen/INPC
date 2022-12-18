import java.io.*;
import java.util.*;
import java.util.ArrayList;
import java.text.DecimalFormat;

/*
Indice Nal Precios Consumidor
https://www.inegi.org.mx/temas/inpc/#Herramientas
https://www.inegi.org.mx/app/preciospromedio/?bs=18

Ciudades: CDMX 01, GDL 04, VER 16, CHIH 19, OAX 38, SAL 53

	Continuacion de INPC2
	Same same but CDMX mayo-sept 2022
	


*/
public class INPC3 {

	static String path = "C:/Users/arrio/Bases de datos/INPC/" ;
	static DecimalFormat df1 = new DecimalFormat("#,##0.00");
	static DecimalFormat df2 = new DecimalFormat("00.00");
	static DecimalFormat df3 = new DecimalFormat("####0.00");
	static DecimalFormat df4 = new DecimalFormat("####0.##");
	
	static final double HUEVOS_POR_KILO = 17d; 
	
	// Dieta 2, estandar Stacy usando BUPA y Health Canada
	static double[] consumo = {
		1.5d,	// Arroz   			1,5
		1.00d,	// Galletas		      1  
		1.50d,	// Harina T			1.5 
		6d,		// Maiz				6	
		2d,		// Harina M			2	
		10d,	// Bolillo 1 = 75 g	0.75	
		1.00d,	// Pasta		    1   
		5.0d,	// Tortilla			5	
		0.45d,	// Cerdo					0.45	        
		0.45d,	// Res						0.45
		0.50d,	// Pollo   		        	0.50
		0.25d,	// Salchicha				0.25
		0.98d,	// Atun 					-----
		0.42d,	// Pescado fresco			1.65
		2.59d,	// Huevo 1 KG 	~17 	
		8d,		// Leche 				
		1.2d,	// queso fresco			
		0.50d,	// Aceites 		     	
		2d,		// Aguacate					2        
		2d,		// Limon  		   			2    
		1.5d,	// Manzana   		  				1.5    
		2d,		// Naranja   						2
		2.5d,	// Papaya  		    	 		   	2.5
		3d,		// Platano		    		    	3
		2d,	// Calabacita		  			2		-----
		1d,	// Cebolla 		    			1    	9
		4.2d,	// Frijol procesado		
		2d,	// Jitomate						2
		0.5d,	// Chiles	            
		1d,	// Papa 	            1
		1d,	// Zanahoria   	        ---		1
		0.9d,	// Azucar     	    19.75	---    
		0.5d	//	Cafe					10

	};
	
	// CDMX 01, GDL 04, VER 16, CHIH 19, OAX 38, SAL 53	
	static String[] ciudades = {"1"};	

	public static String id_ciudad(int c) throws Exception {
		if (c==1) return "CDMX";
		if (c==4) return "GDL";
		if (c==16) return "VERACRUZ";
		if (c==19) return "CHIHUAHUA";
		if (c==38) return "OAXACA";   
		if (c==53) return "SALTILLO";
		throw new Exception("Ciudad desconocida");
	}
	
	public static String id_ciudad_2(int c) throws Exception { return id_ciudad(c).substring(0,3);	}

	static String file1 = "inflacion tabla productos3.csv";
	static String file2 = "inflacion tabla canasta3.csv";
	
	// args[0] true for printing
	// args[1] is to select avg calculation
	// ==1 cheapest
	// ==null or 2, suma2 cheapest n/2+1
	// ==3 suma3, avr of all
	public static void main(String[] args) throws Exception { 
		int avg = 0;
		if (args.length != 2) avg = 2;
		else avg = Integer.parseInt(args[1]);  
		PrintStream out1 = System.out;
		PrintStream out2 = System.out;
		if (args[0].equals("true")) {
			out1 = new PrintStream(new File(path+file1),"UTF-8");
			out2 = new PrintStream(new File(path+file2),"UTF-8");
		}
		out1.println("ciudad,cve_prod,desc_prod,fecha,precio,incr");
		out2.println("ciudad,fecha,cfin,incr");
		for (String c:ciudades) {
			Tabla_INPC	tbl = new Tabla_INPC(c);
			run(tbl,c,out1,out2,avg);
		}
		out1.flush();
		out2.flush();
		out1.close();
		out2.close();
	}

	// Normaliza huevo: normaliza precio a 1 kg
	// Normaliza atun: no, ya viene normalizado
	static double normaliza(int ciudad, int ano, int mes, int cve, String spec,double precio, String unit) throws Exception {
	 int n = 0;
	 try {
		if (cve==8 && unit.equals("PZA")) return precio;					// bolillo no se normaliza
		if (cve==36 && unit.equals("LT")) return precio;					// leche
		if (cve==43 && unit.equals("LT")) return precio;					// aceite

		// from here on, everthing must be either egg or come in KG
		if (unit.equals("KG")) 				return precio;
		if (spec.indexOf("PAQ DE 1 KG")!=-1)return precio;
		if (cve!=32)						return -1;						// ignore
		
		// eggs
		if (unit.equals("DOCENA"))		return precio*HUEVOS_POR_KILO/12;	// 1 KG = 17 huevos
		
		int i = spec.indexOf("PZAS");										// "PAQ DE 30 PZAS", " PAQ C/18 PZAS"
		if (i!=-1) {
			n = Integer.parseInt(spec.substring(i-3,i-1));
			return (precio*HUEVOS_POR_KILO/n);
		}
		
		i = spec.indexOf("CAJA DE");
		if (i!=-1) {
			n = Integer.parseInt(spec.substring(i+8,i+10));
			return (precio*HUEVOS_POR_KILO/n);
		}

		i = spec.indexOf("PAQ DE ");
		if (i!=-1) {
			n = Integer.parseInt(spec.substring(i+7,i+9));
			return (precio*HUEVOS_POR_KILO/n);
		} 
		
		return -1;
		
	 } catch (Exception e) {  return -1; }
	 
	 
	} // normaliza

	// Avg of prices AFTER excluding up to the top 2 prices, as long as at least 3 remain
	// Superseded
	static double suma(List<Double> lista ) throws Exception {
		int n = 0;
		while (n<2 && lista.size()>3) {	// 3 min
			double max = 0; int maxi = 0;
			for (int i=0; i<lista.size(); i++) if (lista.get(i)>max) { max=lista.get(i); maxi=i; }
			lista.remove(maxi); 
			n++;
		}
		double s = 0; 
		for (int i=0; i<lista.size(); i++) s = s+lista.get(i);
		double avg = s/lista.size(); 
		return avg;
	}
	
	// Cheapest
	static double suma1(List<Double> list ) throws Exception {
		if (list.size()==0) throw new Exception("Null list");
		Collections.sort(list); // ascending order
		return list.get(0);
	}

	// Cheaper half plus 1, min 3 DEFAULT
	static double suma2(//int ciudad,int anoi,int mesi,int cvei, 
	List<Double> list ) throws Exception {
		//System.out.print(list.size()+"\t"); //Arrays.toString(list.toArray()));
		//System.out.println("----------"+ciudad +"/"+ anoi +"/"+ mesi +"/"+ cvei+ "\t" + Arrays.toString(list.toArray()));
		if (list.size()==0) throw new Exception("Null list");
		int n = list.size(); if (n>3) n = list.size()/2+1; 
		Collections.sort(list); // ascending order
		double s = 0; 
		for (int i=0; i<n; i++) s = s+list.get(i);
		return s/n;
	}

	// The whole enchilada
	static double suma3(List<Double> list ) throws Exception {
		if (list.size()==0) throw new Exception("Null list");
		int n = list.size(); 
		double s = 0; 
		for (int i=0; i<n; i++) s = s+list.get(i);
		return s/n;
	}

	// ff: input file name on disk, CDMX or "CINCO CIUDADES"
	// c: id ciudad
	public static void run(Tabla_INPC tbl, String c, PrintStream out1, PrintStream out2, int avg) throws Exception { 
		int ciudad = Integer.parseInt( c );

		double[][][] tabla		= new double[5][12][98];		// año x mes x clave prod
		String[]	 descr		= new String[98];				// descr
		int ano = 0; int mes = 0; int cve = 0; String f10 = null; double avg_c = 0;

		List<Double> lista = new ArrayList<Double> ();		
		String[] f = tbl.get(0);
		int anoi 			= Integer.parseInt( f[0] );
		int mesi 			= Integer.parseInt( f[1] );
		int cvei			= Integer.parseInt( f[9] );
		String f10i			= f[12];
		for (int i=0; i<tbl.size(); i++) {
			f = tbl.get(i);
			//System.out.println(Arrays.toString(f));
			ano 			= Integer.parseInt( f[0] );
			mes 			= Integer.parseInt( f[1] );
			cve				= Integer.parseInt( f[9] ); 
			f10 			= f[10];
			String spec			= f[12].trim();
			double precio	 	= Double.parseDouble( f[13] );
			String unidad		= f[15].trim();
			precio = normaliza(ciudad,ano,mes,cve,spec,precio,unidad);	// huevo
			//System.out.println(ciudad+"/"+ano+"/"+mes+"/"+cve+"/"+spec+"/"+precio+"\t"+unidad);
			if (precio==-1) continue;						// issue en precio, ignora
			if (avg!=3) {
				if (f[12].indexOf("OLIVA")!=-1) 		continue;
				if (f[12].indexOf("MARGARINA")!=-1) 	continue;
				if (f[12].indexOf("BETABEL")!=-1) 		continue;
				if (f[12].indexOf("RABANOS")!=-1) 		continue;
				if (f[12].indexOf("CAMBRAY")!=-1) 		continue;
				if (f[12].indexOf("MARGARINA")!=-1) 	continue;
				if (f[12].indexOf("BAGUETTE")!=-1) 		continue;
				if (f[12].indexOf("BARRA, FIGURA")!=-1) continue;
				//if (f[12].indexOf("SARDINA")!=-1) continue;
				//if (f[12].indexOf("MAIZENA")!=-1) continue;
				//if (f[12].indexOf("MAiCENA")!=-1) continue;
			}
			
			//int im = Math.min(10,Math.min(f10i.length(),f10.length()));
			//if ( !f10i.substring(0,im).equals(f10.substring(0,im)) ) System.out.println("CVE = "+cvei+" "+f10i);
				
			if ( anoi!=ano || mesi!=mes || cvei!=cve ) {		// corte 
				if (avg==1) avg_c = suma1(lista) ;
				if (avg==2) avg_c = suma2(lista) ;
				if (avg==3) avg_c = suma3(lista) ;
				
				tabla[anoi-2018][mesi-1][cvei-1] = avg_c;  //suma2(ciudad,anoi,mesi,cvei,lista);
				lista.clear();
			}
			lista.add(precio);
			descr[cve-1]		= f[10];
			anoi=ano; mesi=mes; cvei=cve; f10i=f10;
		}
		if (avg==1) avg_c = suma1(lista) ;
		if (avg==2) avg_c = suma2(lista) ;
		if (avg==3) avg_c = suma3(lista) ;
		tabla[ano-2018][mes-1][cve-1] = avg_c; // suma2(ciudad,anoi,mesi,cvei,lista);
		//System.out.println("CVE = "+cve+" "+f10);
		
		tabla_productos(ciudad,tabla,descr,out1);	
		//System.out.println();
		//costo_unitario(ciudad,2018,12,2022,7,tabla,descr);
		//System.out.println();
		//costo_consumo(ciudad,2018,12,2022,7,tabla,descr,true);
		//System.out.println();
		//costo_consumo(ciudad,2021, 7,2022,7,tabla,descr,true);		
		
		// Costo canasta

		int ainf = 2022; int asup = 2022;  int minf=5; int msup=10; 
		double c_ini=0;
		for (int a=ainf; a<=asup; a++) {
			for (int m=minf; m<msup; m++) {
				double c_fin = costo_consumo(ciudad,ainf,minf,a,m,tabla,descr,false); // true para imprimir detalle
				if (c_ini==0) c_ini=c_fin;
				double incr = (c_fin/c_ini - 1)*100;
				out2.println(
					id_ciudad_2(ciudad) + "," + a + "-" + m + "-01,"+
					df4.format(c_fin) + ","+	// df3
					df4.format(incr) );			// df2
				//if ( (a==ainf && m==minf) || (a==asup && m==(msup-1)) ) 
				System.out.println(
					id_ciudad_2(ciudad) + "," + a + "-" + m + "-01,"+
					df4.format(c_fin) + ","+	// df3
					df4.format(incr) );			// df2
	
			}
		}
	} // main
		
	static void tabla_productos(int ciudad, double[][][] tabla, String[] descr, PrintStream out) throws Exception {	

		int ainf = 2022; int asup = 2022;  int minf=5; int msup=10; 
		for (int c=1; c<99; c++) {
			double init = 0;
			if (descr[c-1]==null) continue;
			//System.out.println("========"+descr[c-1]);
			for (int a=ainf; a<=asup; a++) {
				for (int m=minf; m<msup; m++) {
					double avg = tabla[a-2018][m-1][c-1];
					//int num = numpr[a-2018][m-1][c-1];
					//double avg = suma/num;
					if (init==0) init = avg;
					double incr = (avg/init - 1)*100;
					String ms = "0"+m; if (m>9) ms = m + "";
					out.println(
						id_ciudad_2(ciudad) + "," + c + "," + descr[c-1] + "," + a + "-" + ms + "-01,"+
						df4.format(avg) + ","+ // df3
						df4.format(incr) );		// df2
				}
			}
		}
	} 

	static void costo_unitario(int ciudad,int aini,int mini,int afin,int mfin,
			double[][][] tabla, String[] descr)  throws Exception {	
		// Only % in totals
		String ff = "%8s";
		String fg = "%-45s";
		String afrom = aini+"/"+mini;String ato = afin+"/"+mfin;
		System.out.println("COSTOS UNITARIOS "+aini+"-"+afin+" "+id_ciudad(ciudad));
		System.out.println("Item                                          "+afrom+"      "+ato+"   % Incremento");
		System.out.println();

		for (int c=1; c<99; c++) {
			if (descr[c-1]==null) continue;
			System.out.print(String.format(fg,descr[c-1]));

			int a = aini-2018; int m = mini;
			double avg1 = tabla[a][m-1][c-1];
			// int num = numpr[a][m-1][c-1];
			// double avg1 = suma/num;
			System.out.print(String.format(ff,df1.format(avg1))+"\t");
			
			a = afin-2018; m = mfin;
			double avg2 = tabla[a][m-1][c-1];
			// num = numpr[a][m-1][c-1];
			// double avg2 = suma/num;
			System.out.print(String.format(ff,df1.format(avg2))+"\t");

			double incr = (avg2/avg1 - 1)*100;
			System.out.print(String.format(ff,df2.format(incr))+"\t");
			
			System.out.println();
		}
	}
	
	static double costo_consumo(int ciudad,int aini,int mini,int afin,int mfin,
			double[][][] tabla, String[] descr, boolean print) throws Exception {	
			
		String ff = "%8s";
		String fg = "%-45s";
		String afrom = aini+"/"+mini;String ato = afin+"/"+mfin;
		
		if (print) {
		System.out.println("COSTOS P/CONSUMO "+aini+"-"+afin+" "+id_ciudad(ciudad));
		System.out.println("Item                                          Consumo      Costo           Costo");
		System.out.println("                                           (unidades)     "+aini+"/"+mini+"          "+afin+"/"+mfin);
		System.out.println();
		}
		
		double s1 = 0; double s2 = 0; int k=0;
		for (int c=1; c<99; c++) {
			if (descr[c-1]==null) continue;
			
			if (print) {
			System.out.print(String.format(fg,descr[c-1]));
			System.out.print(String.format(ff,df1.format(consumo[k]))+"\t");
			}
			
			int a = aini-2018; int m = mini;
			double avg1 = tabla[a][m-1][c-1];
			// int num = numpr[a][m-1][c-1];
			// double avg1 = suma/num;
			s1 = s1 + avg1*consumo[k];
			if (print) System.out.print(String.format(ff,df1.format(avg1*consumo[k]))+"\t");
			
			a = afin-2018; m = mfin;
			double avg2 = tabla[a][m-1][c-1];
			// num = numpr[a][m-1][c-1];
			// double avg2 = suma/num;
			s2 = s2 + avg2*consumo[k];
			if (print) System.out.print(String.format(ff,df1.format(avg2*consumo[k]))+"\t");

			if (print) System.out.println();
			k++;
		}
		double incr2 = (s2/s1 - 1)*100;
		
		if (print) {
		System.out.println();
		System.out.print(String.format(fg,"COSTO TOTAL"));
		System.out.print("\t\t");
		System.out.print(String.format(ff,df1.format(s1)+"\t"));
		System.out.print(String.format(ff,df1.format(s2)+"  INCR = "));
		System.out.print(String.format("%6s",df2.format(incr2))+"%");
		System.out.println();
		}
		return s2;
	}
	


} // class

// Aux, couldnt download it all at once
class Tabla_INPC {
	
	List<Linea_Tabla> ts = null;
	
	public Tabla_INPC(String c) throws Exception {
		ts = new ArrayList<Linea_Tabla> ();
		lee(c,ts,INPC3.path+"INPC may 2022 - sept 2022 CDMX.csv","#");
		lee(c,ts,INPC3.path+"INPC may 2022 - sept 2022 CDMX galletas.csv","#");
	}
		
	String[] get(int i) { return ts.get(i).f;}
	int size() { return ts.size();}
		
	// Reads registers of city c in a cvs file; adds them to pre-existing table t
	public static void lee(String c, List<Linea_Tabla> t, String fileName, String separator) throws Exception {
	  String data = null; 
	  try {
		int ci = Integer.parseInt(c);
		BufferedReader in1 = new BufferedReader (new InputStreamReader (new FileInputStream(fileName)));
		data = in1.readLine(); // skip header
		data = in1.readLine();
		data = in1.readLine();
		data = in1.readLine();
		data = in1.readLine();
		data = in1.readLine();		
		while ((data = in1.readLine())!=null) {
			data = data.replace("BARILLA, CODO #4","BARILLA"); 
			data = data.replace("FIDEOS, # 2","FIDEOS");
			String[] ss = data.split(separator);
			if (Integer.parseInt(ss[3])==ci) t.add(new Linea_Tabla(ss)) ;
		}
		in1.close();
	  } catch(Exception e) {
		    System.out.println(e);
		    System.out.println(data);
			System.out.println(c+"/"+fileName);
	  }
	} 	
}

class Linea_Tabla {
	String[] f;
	public Linea_Tabla(String[] f) { this.f=f; }
	int length() { return f.length; }
}


