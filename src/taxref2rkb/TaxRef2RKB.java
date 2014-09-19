/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package taxref2rkb;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author murloc
 */
public class TaxRef2RKB {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) 
    {
        SparqlProxy spOut = SparqlProxy.getSparqlProxy("http://amarger.murloc.fr:8080/TaxRef2RKB_out_TEST/");
        
        System.out.println("Clear previous Sparql Endpoint...");
        spOut.clearSp();
        System.out.println("Sparql Endpoint cleared!");
        
        TaxRefExtractor tre = new TaxRefExtractor(spOut, "in/agronomicTaxon.owl");
        tre.loadRef("in/TAXREFv70_utf8.txt");
        
        System.out.println("Filtering sub part ...");
        tre.filterSubPart("http://inpn.mnhn.fr/espece/cd_nom/187079");
        System.out.println("Sub part filtered");
        
        
        String dateFileName = new SimpleDateFormat("dd-MM_HH-mm_").format(new Date());
        System.out.println("Exporting RKB to file : out/"+dateFileName+"_TaxRef_OWL.owl");
        tre.exportRefToFile(dateFileName+"_TaxRef_OWL"); 
        System.out.println("File generated");
    }
    
}
