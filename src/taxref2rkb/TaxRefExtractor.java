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
    }
    
    public String getAlign(String rank)
    {
        String ret = this.aligns.get(rank);
        
        if(ret == null)
        {
            ret = "http://ontology.irstea.fr/AgronomicTaxon#Taxon";
        }
        
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
                    currentQueryPart += "<"+uriRef+"> rdf:type <"+this.getAlign(rang)+">;";
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
                  currentQueryPart += "<http://ontology.irstea.fr/AgronomicTaxon#hasScientificName> \""+scientificLabel+"\"; ";
                  nbLabels ++;
                }
                if(!vernLabelFR.isEmpty())
                {
                    currentQueryPart += "<http://ontology.irstea.fr/AgronomicTaxon#hasVernacularName> \""+vernLabelFR+"\"@fr ;";
                    nbLabels++;
                }
                if(!vernLabelEN.isEmpty())
                {
                    currentQueryPart +="<http://ontology.irstea.fr/AgronomicTaxon#hasVernacularName> \""+vernLabelEN+"\"@en ;";
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
        
        
        ret += "}";
        return ret;
    }
     
     
     public void filterSubPart(String uriTop)
     {
         this.spOut.storeData(new StringBuilder("DELETE {?uri ?a?b. ?a?b ?uri.} WHERE{ FILTER NOT EXISTS { ?uri <http://ontology.irstea.fr/AgronomicTaxon#hasHigherRank> <"+uriTop+">. } }"));
     }
    
}
