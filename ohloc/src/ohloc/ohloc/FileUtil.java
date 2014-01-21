package ohloc.ohloc;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

public class FileUtil {
	
	
	
	public List<File> listFiles (File directory,  String filterExtFile) {  
        List<File> files = new ArrayList<File>();  
        listFiles (files, directory, filterExtFile);  
        return files;  
    }  
      
    private void listFiles (List<File> files, File directory, final String filterExtFile) {  
    	
		FilenameFilter filtro = new FilenameFilter() { // Isso é um inner Class.. Este é um Filtro para Leitura de arquivos
			public boolean accept(File dir, String name) {
				return !name.endsWith(filterExtFile); // aqui você coloca seu filtro... Neste caso todos os que termine com a estencao .txt
			}
		};
    	
        for (File file : directory.listFiles( filtro )) {  
            if (file.isDirectory()) {  
                listFiles (files, file, filterExtFile);  
            } else {  
                files.add (file);  
            }  
        }  
    } 

}
