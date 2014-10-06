/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package taxref2rkb;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.mozilla.universalchardet.UniversalDetector;

/**
 *
 * @author murloc
 */
public class TaxRefExtractor 
{
    
    SparqlProxy spOut;
    HashMap<String, String> aligns;
    HashMap<String, String> rankLabels;
    String adomFileName;
    
    public TaxRefExtractor(SparqlProxy spOut, String adomFileName)
    {
        this.adomFileName = adomFileName;
        this.spOut = spOut;
        this.aligns = new HashMap<>();
        this.aligns.put("KD", "http://ontology.irstea.fr/AgronomicTaxon#KingdomRank");
        this.aligns.put("PH", "http://ontology.irstea.fr/AgronomicTaxon#PhylumRank");
        this.aligns.put("CL", "http://ontology.irstea.fr/AgronomicTaxon#ClassRank");
        this.aligns.put("OR", "http://ontology.irstea.fr/AgronomicTaxon#OrderRank");
        this.aligns.put("FM", "http://ontology.irstea.fr/AgronomicTaxon#FamilyRank");
        this.aligns.put("GN", "http://ontology.irstea.fr/AgronomicTaxon#GenusRank");
        this.aligns.put("ES", "http://ontology.irstea.fr/AgronomicTaxon#SpecyRank");
        this.aligns.put("VAR", "http://ontology.irstea.fr/AgronomicTaxon#VarietyRank");
        
        
        this.rankLabels = new HashMap<>();
        this.rankLabels.put("Dumm", "Domaine");
        this.rankLabels.put("OR", "Ordre");
        this.rankLabels.put("AGES", "Agrégat");
        this.rankLabels.put("SPRG", "Super‐Règne");
        this.rankLabels.put("SBOR", "Sous‐Ordre");
        this.rankLabels.put("ES", "Espèce");
        this.rankLabels.put("KD", "Règne");
        this.rankLabels.put("IFOR", "Infra‐Ordre");
        this.rankLabels.put("SMES", "Semi‐Espèce");
        this.rankLabels.put("SSRG", "Sous‐Règne");
        this.rankLabels.put("SPFM", "Super‐Famille");
        this.rankLabels.put("MES", "Micro‐Espèce");
        this.rankLabels.put("IFRG", "Infra‐Règne");
        this.rankLabels.put("FM", "Famille");
        this.rankLabels.put("SSES", "Sous‐Espèce");
        this.rankLabels.put("PH", "Phylum/Embranchement");
        this.rankLabels.put("SBFM", "Sous‐Famille");
        this.rankLabels.put("NAT", "Natio");
        this.rankLabels.put("SBPH", "Sous‐Phylum");
        this.rankLabels.put("TR", "Tribu");
        this.rankLabels.put("VAR", "Variété");
        this.rankLabels.put("IFPH", "Infra‐Phylum");
        this.rankLabels.put("SSTR", "Sous‐Tribu");
        this.rankLabels.put("SVAR", "Sous‐Variété");
        this.rankLabels.put("DV", "Division");
        this.rankLabels.put("GN", "Genre");
        this.rankLabels.put("FO", "Forme");
        this.rankLabels.put("SBDV", "Sous‐division");
        this.rankLabels.put("SSGN", "Sous‐Genre");
        this.rankLabels.put("SSFO", "Sous‐Forme");
        this.rankLabels.put("SPCL", "Super‐Classe");
        this.rankLabels.put("SC", "Section");
        this.rankLabels.put("FOES", "Forma species");
        this.rankLabels.put("CLAD", "Cladus");
        this.rankLabels.put("SBSC", "Sous‐Section");
        this.rankLabels.put("LIN", "Linea");
        this.rankLabels.put("CL", "Classe");
        this.rankLabels.put("SER", "Série");
        this.rankLabels.put("CLO", "Clône");
        this.rankLabels.put("SBCL", "Sous‐Classe");
        this.rankLabels.put("SSER", "Sous‐Série");
        this.rankLabels.put("RACE", "Race");
        this.rankLabels.put("IFCL", "Infra‐classe");
        this.rankLabels.put("CAR", "Cultivar");
        this.rankLabels.put("LEG", "Legio");
        this.rankLabels.put("MO", "Morpha");
        this.rankLabels.put("SPOR", "Super‐Ordre");
        this.rankLabels.put("AB", "Abberatio");
        this.rankLabels.put("COH", "Cohorte");
        this.rankLabels.put("HYB", "Hybride");
        
    }
    
    public String getAlign(String rank)
    {
        String ret = this.aligns.get(rank);
        
        /*if(ret == null)
        {
            ret = "http://ontology.irstea.fr/AgronomicTaxon#Taxon";
        }*/
        
        return ret;
    }
    
    public void loadRef(String fileName)
    {
         System.out.println("Delete previous KB on endpoint ...");
        this.spOut.storeData(new StringBuilder("DELETE WHERE { ?a ?b ?c.}"));
        System.out.println("Endpoint empty.");
        
        System.out.println("Exporting  ADOM...");
        this.spOut.storeData(new StringBuilder(this.getADOMTtl(adomFileName)));
        System.out.println("ADOM exported!");
        
        System.out.println("Treating ref ...");
        Path path = Paths.get(fileName);
        try (Scanner scanner =  new Scanner(path))
        {
             StringBuilder updateQuery = new StringBuilder("INSERT DATA {");
            int nbQuery = 0;
            int i  = 0;
            int nbLabels = 0;
            scanner.next(); //skip first line
          while (scanner.hasNextLine())
          {
              String currentQueryPart = "";
              
              String line = scanner.nextLine();
              String[] params = line.split("[\t]");
              String id = params[7];
              String idSup = params[8];
              String idRef = params[9];
              String rang = params[10];
              String scientificLabel = SparqlProxy.cleanString(params[11]);
              String vernLabelFR = SparqlProxy.cleanString(params[15]);
              String vernLabelEN = SparqlProxy.cleanString(params[16]);
              String url = params[33];
              
              String typeTaxon = params[0];
              
              if(typeTaxon.compareToIgnoreCase("Plantae") == 0)
              {
                String uriRef = "";
                 uriRef = "http://inpn.mnhn.fr/espece/cd_nom/"+idRef;
                 
                 
                if(id.compareTo(idRef) == 0)
                {
                    String rankUri = this.getAlign(rang);
                    if(rankUri == null)
                    {
                        System.out.println("NEW RANK : "+rang);
                        rankUri = "http://inpn.mnhn.fr/espece/cd_nom/"+rang;
                        String label = rang;
                        if(this.rankLabels.containsKey(rang))
                        {
                            label = this.rankLabels.get(rang);
                        }
                        currentQueryPart += "<"+rankUri+"> rdf:type owl:Class; rdfs:subClassOf  <http://ontology.irstea.fr/AgronomicTaxon#Taxon>; rdfs:label \""+label+"\".  ";
                        this.aligns.put(rang, rankUri);
                    }
                    currentQueryPart += "<"+uriRef+"> rdf:type <"+rankUri+">;";
                    if(!idSup.isEmpty())
                        currentQueryPart += "<http://ontology.irstea.fr/AgronomicTaxon#hasHigherRank> <http://inpn.mnhn.fr/espece/cd_nom/"+idSup+">; ";
                    i++;
                }
                else
                {
                    currentQueryPart += "<"+uriRef+"> ";
                }

                if(!scientificLabel.isEmpty())
                {
                  currentQueryPart += " <http://ontology.irstea.fr/AgronomicTaxon#hasScientificName> \""+scientificLabel+"\"; ";
                  currentQueryPart += " rdfs:label \""+scientificLabel+"\"; ";
                  nbLabels ++;
                }
                if(!vernLabelFR.isEmpty())
                {
                    currentQueryPart += "<http://ontology.irstea.fr/AgronomicTaxon#hasVernacularName> \""+vernLabelFR+"\"@fr ;";
                    currentQueryPart += "rdfs:label \""+vernLabelFR+"\"@fr ;";
                    nbLabels++;
                }
                if(!vernLabelEN.isEmpty())
                {
                    currentQueryPart +="<http://ontology.irstea.fr/AgronomicTaxon#hasVernacularName> \""+vernLabelEN+"\"@en ;";
                    currentQueryPart +="rdfs:label \""+vernLabelEN+"\"@en ;";
                    nbLabels++;
                }
                currentQueryPart += ". ";

                int fullLength = updateQuery.length()+currentQueryPart.length();
                  if(fullLength > 3000000) // limit for the fuseki update query length
                  {
                      nbQuery ++;
                      updateQuery.append("}");
                      boolean ret = this.spOut.storeData(updateQuery);
                      System.out.println(i+" refs treated (query n° "+nbQuery+")...");
                      if(!ret) //if store query bugged
                      {
                          System.exit(0);
                      }
                      updateQuery = new StringBuilder("INSERT DATA {"+currentQueryPart);
                  }
                  else
                  {
                      updateQuery.append(currentQueryPart);
                  }
              } // if plantae
          }
          nbQuery ++;
            updateQuery.append("}");
            boolean ret = this.spOut.storeData(updateQuery);
            System.out.println(i+" refs treated (query n° "+nbQuery+")...");
            if(!ret) //if store query bugged
                System.exit(0);
            System.out.println("All ref are exported!");
            System.out.println(i+" refs");
            System.out.println(nbLabels+" labels");
        } 
        catch (IOException ex) 
        {
            System.out.println("Error during reading taxRef file!");
            System.exit(0);
        }
    }
    
    public void exportRefToFile(String fileName)
    {
        this.spOut.writeKBFile(fileName);
    }
    
    
     public String getADOMTtl(String adomFileName)
    {
        
        String ret = "prefix : <http://www.w3.org/2002/07/owl#> \n" +
"prefix owl: <http://www.w3.org/2002/07/owl#> \n" +
"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
"prefix xml: <http://www.w3.org/XML/1998/namespace> \n" +
"prefix xsd: <http://www.w3.org/2001/XMLSchema#> \n" +
"prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> ";
        
        ret += "INSERT DATA {";
        
        try 
        {
            ret +=  IOUtils.toString( new FileInputStream(new File(adomFileName)));
            //ret = ret.replaceAll("^@.+\\.$", "");   // remove Prefix (wrong syntax for SPARQL insert query)
        } 
        catch (IOException ex) 
        {
            System.err.println("Can't read adom file!");
            System.exit(0);
        }
        catch(Exception e)
        {
            System.err.println("ERROR : "+e);
            System.exit(0);
        }
        
        
        ret += "}";
        return ret;
    }
     
     
     public void filterSubPart(String uriTop)
     {
         this.spOut.storeData(new StringBuilder(" DELETE {?uri ?a ?b. ?a ?b ?uri.} WHERE{"
                 + "FILTER ( ?uri != <"+uriTop+">)"
                 + " FILTER NOT EXISTS { ?uri <http://ontology.irstea.fr/AgronomicTaxon#hasHigherRank>+ <"+uriTop+">. }"
                 + "FILTER NOT EXISTS { <"+uriTop+"> <http://ontology.irstea.fr/AgronomicTaxon#hasHigherRank>+ ?uri}"
                 + " }"));
         
         //DELETE classes not used
         this.spOut.storeData(new StringBuilder(" DELETE {?uri ?a ?b. ?a ?b ?uri.} WHERE{"
                 + " FILTER NOT EXISTS { ?uri rdf:type <"+uriTop+">. }"
                 + " }"));
     }
    
}
