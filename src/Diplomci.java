import java.sql.*;
import java.util.ArrayList;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.io.File;

public class Diplomci
{   

	static
	{
		try{
		  Class.forName("com.ibm.db2.jcc.DB2Driver");
		}
		catch (Exception e) {
		  e.printStackTrace();
		}
	}

	

	public static void main(String[] args) throws SQLException
	{
		
	  //Kreiramo prvi panel
	  final JFrame frame1 = new JFrame();
	  frame1.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	  frame1.setTitle("Pretraga studenata");
		
	  final JPanel panel1 = new JPanel();
	  final JLabel label1 = new JLabel("Unesite godinu upisa: ");
	  final JTextField text1 = new JTextField(15);
	  text1.setToolTipText("Unesite godinu upisa, npr. 2010");
	  final JLabel label2 = new JLabel("Unesite indeks studenta: ");
	  final JLabel errlabel = new JLabel("                         ");
	  errlabel.setFont(new Font("Serif", Font.ITALIC, 14));
	  errlabel.setForeground(Color.red);
	  final JTextField text2 = new JTextField(15);
	  text2.setToolTipText("Unesite indeks, npr. 20100123");
	  final JButton dugme1 = new JButton();
	  dugme1.setText("Potvrdi");
        
	  //Postavljamo logo fakulteta kao labelu 
	  frame1.setLayout(new BorderLayout());
	  JLabel logo = new JLabel(new ImageIcon("logomatf.jpg"));
	  logo.setSize(50, 50);
	  logo.setLayout(new FlowLayout());
	  frame1.add(logo);
	     
		
	  panel1.add(label1);
	  panel1.add(text1);
	  panel1.add(label2);
	  panel1.add(text2);
	  panel1.add(dugme1);
	  panel1.add(errlabel);
	  panel1.add(logo);
	  panel1.setBackground(Color.white);
        
	  frame1.setSize(195, 300);
	  panel1.setSize(195, 300);
	  frame1.add(panel1);
	  frame1.setResizable(false);
	  frame1.setVisible(true);
	  frame1.setLocationRelativeTo(null);
        
       
        
        // Definisemo akcije pri kliku na dugme za unos podataka
        dugme1.addActionListener(new ActionListener() 
		{
        
			public void actionPerformed(ActionEvent ae) 
			{
				try 
				{   
					//Konekcija na bazu
					Connection con = null;
					String url = "jdbc:db2://localhost:50001/vstud";
					con = DriverManager.getConnection(url, "student", "abcdef");
					con.setAutoCommit(false);
					
					
					String diplomirani = "with indeksbodova as " +
							"(select indeks, sum(p.bodovi) polozeno, dec(avg(ocena*1.0), 4, 2) prosek " +
							"from ispit i join predmet p " +
							"on i.id_predmeta = p.id_predmeta " +
							"where ocena > 5 and status_prijave = 'o' " +
							"group by indeks " +
							"having sum(p.bodovi) >= 240) " +
							"select trim(ime) imee, trim(prezime) pr, ib.polozeno, ib.prosek, d.datum_rodjenja, trim(d.mesto_rodjenja) mr, trim(s.naziv) smern, d.pol, d.ime_oca " +
							"from indeksbodova ib join dosije d " +
							"on d.indeks = ib.indeks " +
							"join smer s on d.id_smera = s.id_smera " +
							"join status ss on ss.indeks = d.indeks " +
							"join obavezan_predmet op on op.id_smera = d.id_smera " +
							"join predmet p on p.id_predmeta = op.id_predmeta " +
							"join ispit i on i.id_predmeta = p.id_predmeta " +
							"join upis_godine ug on ug.indeks = d.indeks " +
							"where year(ug.datum_upisa) = ? " +
							"and ib.indeks = ? "+
							"and ss.status = 'diplomirao' " +
							"and ug.datum_upisa = (select min(datum_upisa) from upis_godine ug1 where ug1.indeks = d.indeks) " +
							"and not exists(select * from ispit i where i.indeks = d.indeks " +
											"and i.id_predmeta = op.id_predmeta " +
											"and ocena > 5 and status_prijave = 'o') " +
							"group by ime, prezime, ib.indeks, ib.polozeno, ib.prosek, d.datum_rodjenja, d.mesto_rodjenja, s.naziv, d.pol, d.ime_oca ";
			
					String ostaliStudenti = "select d.indeks, trim(ime) im, trim(prezime) pr, trim(p.naziv) prnaziv " +
							 "from dosije d join ispit i " +
							 "on d.indeks = i.indeks " +
							 "join predmet p on p.id_predmeta = i.id_predmeta " +
							 "join status st on st.indeks = d.indeks " +
							 "join upis_godine ug on ug.indeks = d.indeks " +
							 "where i.indeks = ? " +
							 "and year(ug.datum_upisa) = ? " +
							 "and d.indeks not in (select i1.indeks from ispit i1 join obavezan_predmet op " +
							   					   "on i1.id_predmeta = op.id_predmeta " +
							   					   "join upisan_kurs uk " + 
							   					   "on uk.id_predmeta = i1.id_predmeta " +
							   					   "where ocena > 5 and status_prijave = 'o') " +
						     "and st.status <> 'diplomirao' " +
							 "and datum = (select max(datum) from status s2 where i.indeks = s2.indeks) " +
							 "and ug.datum_upisa = (select min(datum_upisa) from upis_godine ug1 where ug1.indeks = d.indeks) " +
							 "group by d.indeks, ime, prezime, p.naziv ";

			
			//Cuvamo podatke koji su uneti preko panela
			String god = text1.getText();
			String ind = text2.getText();
			int godint = Integer.parseInt(god);
			int indint = Integer.parseInt(ind);
			
			if(god.length() < 4 || ind.length() < 8 || (god.length() < 4 && ind.length() < 8))
			{
				errlabel.setText("Pogresan format unosa!");
		        
				text1.setText("");
				text2.setText("");

			}
			else if(godint > 2015)
			{
				errlabel.setText("Uneta godina nije validna!");
		        
				text1.setText("");
				text2.setText("");
			}
			else{
			//Ako je unos prosao bez gresaka
				
			
			//Upiti sadrze upitnike koji su markeri parametara koje unosimo u sledecem koraku
			PreparedStatement ps = con.prepareStatement(diplomirani);
			PreparedStatement ps1 = con.prepareStatement(ostaliStudenti);
			
			//Sada ce figurisati podaci koji su uneti preko panela
			ps.setInt(1, Integer.parseInt(god));
			ps.setInt(2, Integer.parseInt(ind));
			ps1.setInt(1, Integer.parseInt(ind));
			ps1.setInt(2, Integer.parseInt(god));
	        
			//Izvrsavamo skupove rezultata
			//Try/catch blok zbog moguceg cekanja u visekorisnickom okruzenju
	        
			ResultSet rs = null;
			ResultSet rs1 = null;
			try {
			    rs = ps.executeQuery(); 
			    rs1 = ps1.executeQuery();
			} catch (SQLException e2) {
			  if(e2.getErrorCode() == -911 || e2.getErrorCode() == -913){
				errlabel.setText("Sacekajte sa unosom!");
				con.rollback();
				rs = ps.executeQuery(); 
				rs1 = ps1.executeQuery();
				
			  }
			}
			
			//Brisemo podatke koje je korisnik uneo kako bi mogao opet da ih unese kasnije
			text1.setText("");
			text2.setText("");
			
			//Checker ce nam proveravati da li postoji student sa podacima koji su uneseni
			int checker = 0;
			while(rs.next()){
			    
				//Cuvamo podatke iz upita kako bismo ih nadalje lakse koristili
				final String ime = rs.getString(1);
				final String prezime = rs.getString(2);
				final int polozeno = rs.getInt(3);
				final String prosek = rs.getString(4);
				final String datumRodj = rs.getString(5);
				final String mestoRodj = rs.getString(6);
				final String smerNaziv = rs.getString(7);
			
				final String polStr = rs.getString(8);
				final char pol = polStr.charAt(0);
				
				final String imeOca = rs.getString(9);
				final char srIme = imeOca.charAt(0);
			   
				//Kreiramo datum u formatu dd.mm.gggg.
			   	String godina = datumRodj.substring(0, 4);
				String mesec = datumRodj.substring(5, 7);
				String dan = datumRodj.substring(8);
				final String realDatum = dan + "." + mesec + "." + godina + ".";
				
				//Cuvamo trenutno vreme i datum kako bismo ih ispisali u uverenju
				String datum = new Timestamp(System.currentTimeMillis()).toString();
				String dann = datum.substring(8, 10);
				String mes = datum.substring(5,7);
				String godd = datum.substring(0,4);
				String ceodatum = dann + "." + mes + "." + godd + ".";
				
				String vreme = datum.substring(11, 16);
				
				
				//Proveravamo kog je pola student i shodno tome formiramo gramaticki ispravan izvestaj
				JTextArea textarea = new JTextArea();
				if(pol == 'm'){
					
				textarea.setText("Matematicki fakultet\n" +
					 	 "Univerzitet u Beogradu\n" +
					 	  "Studentski trg 16\n\n\n" +
					 	  "\t      UVERENJE O DIPLOMIRANJU\n\n\n" +
						  "Student " + ime + " " + srIme + "." + " " + prezime  + "," + 
						  " sa brojem indeksa " + indint + "," + "\nrodjen u mestu " +
						  mestoRodj + " " +realDatum+ " godine," + "\nupisao je studije na smeru " +
						  smerNaziv + " " + godint + "." + "\ngodine." + "\nProsek studenta je " + 
						  prosek + " i polozio je " + polozeno + " ESPB. " + "\n\n\n" + 
						  "\t\t\t      U Beogradu" + "\n" + "\t\t\t      " + ceodatum + " " + "\n" + "\t\t\t      " 
						  + vreme );
				   
				}
				else{
			    textarea.setText("Matematicki fakultet\n" +
					     "Univerzitet u Beogradu\n" +
					     "Studentski trg 16\n\n\n" +
					     "\t      UVERENJE O DIPLOMIRANJU\n\n\n" +
					     "Studentkinja " + ime + " " + srIme + "." + " " + prezime  + "," + 
					     " sa brojem indeksa " + indint + "," + "\nrodjena u mestu " +
					     mestoRodj + " " + realDatum + " godine," + "\nupisala je studije na smeru " +
					     smerNaziv + " " + godint + "." + "godine. " + "\nProsek studentkinje je " + 
					     prosek + " i polozila je " + polozeno + " ESPB. " + "\n\n\n" + 
					     "\t\t\t    U Beogradu" + "\n" + "\t\t\t    " + ceodatum + " " + "\n" + "\t\t\t    " 
					     + vreme );
				}
				
				//Kreiramo drugi panel na kome ce se ispisati uverenje o diplomiranom studentu
				final JFrame frame2 = new JFrame();
				final JPanel panel2 = new JPanel();
				final JButton dugme2 = new JButton();
			    
				dugme2.setText("Stampaj");
				frame2.setSize(450, 350);
				frame2.setTitle("Uverenje o diplomiranju");
				panel2.setSize(400, 250);
				textarea.setEditable(false);
				Dimension d = new Dimension(400, 270);
				textarea.setPreferredSize(d);
			    
				panel2.add(textarea);
				panel2.add(dugme2);
				frame2.add(panel2);
			
				errlabel.setText("                         ");
				frame2.setVisible(true);
				frame2.setLocationRelativeTo(null);
				
				//Cuvamo uverenje
				final String strUverenje = new String(textarea.getText()); 
				
				//Definisemo akciju pri kliku na dugme za stampanje izvestaja 
				ActionListener a1 = new ActionListener() {
					public void actionPerformed(ActionEvent e1) {
						 if (e1.getSource() == dugme2){
									
									try {
										//Stampamo uverenje u tekstualni fajl uverenja.diplomci
										File uverenje = new File("uverenja.diplomci.txt");
										FileWriter writer = new FileWriter(uverenje);
										writer.write(strUverenje);
										writer.close();
										
									} catch (IOException e) {
										
										e.printStackTrace();
										
									}
								}
							}
						};
				 
				    dugme2.addActionListener(a1); 
				    
				    checker = 1;
				    
				}
			
			 //Kreiramo treci panel na kome se izlistati nepolozeni predmeti studenta
			 final JFrame frame3 = new JFrame();
			 final JLabel label3 = new JLabel("Nepolozeni predmeti");
			 final JPanel panel3 = new JPanel();
			 final JTextArea textarea2 = new JTextArea();
			
			 frame3.setSize(350, 350);
			 frame3.setTitle("Spisak nepolozenih predmeta");
			 Dimension d = new Dimension(280, 280);
			 textarea2.setPreferredSize(d);
			 textarea2.setEditable(false);
		    
			 panel3.add(label3);
			 panel3.add(textarea2);
			 frame3.add(panel3);
			 panel3.setVisible(true);
		     
			 frame3.setResizable(false);
			 frame3.setLocationRelativeTo(null);
		    
		     
			 while(rs1.next()){
				
				frame3.setVisible(true);
			    				
				// Cuvamo sve nepolozene predmete kako bismo ih ispisali  	 
				ArrayList<String> predmeti = new ArrayList<String>();
				predmeti.add(rs1.getString(4));
				String[] predstr = (String[]) predmeti.toArray(new String[predmeti.size()]);
				   
				//u petlji nadovezujemo nazive predmeta i pravimo spisak 
				for(int i=0; i<predstr.length; i++){
					  textarea2.append(Integer.toString(textarea2.getLineCount()) + ". ");
					  textarea2.append(predstr[i]);
					  textarea2.append("\n");
					
				  }
				  
			   checker = 1;
			   errlabel.setText("                         ");
			    
			}
			
			//Proveravamo da li smo nasli studenta
			if(checker == 0){
			  errlabel.setText("Student nije pronadjen!");	 
			}
		 
		   
			con.commit();
			//zatvaramo oba skupa rezultata i konekciju
			rs.close();
			rs1.close();
			con.close();
			}
					} 
					catch (SQLException e) 
					{
						System.out.println("SQLCODE: " + e.getErrorCode() + "\n" + "SQLSTATE: " + e.getSQLState() + "\n" + "MESSAGE: " + e.getMessage());
					  
					}
					catch (Exception e) 
					{   
						e.printStackTrace();
						
						//Ispisujemo poruku korisniku da podaci koje je uneo nisu validni
						errlabel.setText("Podaci nisu validni!");
						
						//Brisemo sadrzaj polja za unos kako bi korisnik mogao opet da unese podatke
						text1.setText("");
						text2.setText("");
				
					}
				}
			});
        
    	   
	}
}
