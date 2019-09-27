package tsurumai.workflow.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;
import java.util.jar.JarFile;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipExtractor{
	protected File src;
	protected ZipFile archive;
	public ZipFile getArchive(){return archive;}
	public ZipExtractor(final File src) throws IOException{
		this.src = src;
		this.archive = new ZipFile(src);
	}
	public ZipEntry getEntry(final String path){
		return archive.getEntry(path);
	}
	@Override
	protected void finalize() throws Throwable {
		if(archive != null){archive.close();}
	}
	public byte[] extract(final String path) throws IOException{
		ZipEntry ent = archive.getEntry(path);
		InputStream in = archive.getInputStream(ent);
		ByteArrayOutputStream buff = new ByteArrayOutputStream();
		for(int read = 0; read < ent.getSize(); ){
			byte[] cur = new byte[1024];
			int len = in.read(cur);
			buff.write(Arrays.copyOfRange(cur, 0, len));
			//System.out.println(String.valueOf(len) + " bytes read.");
			read += len;
		}
		long crc = ent.getCrc();
		CRC32 crc2 = new CRC32();
		byte[] data = buff.toByteArray();
		crc2.update(data);
		if(crc != crc2.getValue())
			throw new IOException("CRC unmatched.");
		return data;

	}
	public String[] list() throws IOException{
		Collection<String> ret = new Vector<String>();
		for(Enumeration<? extends ZipEntry> entries = new ZipFile(this.src).entries(); entries.hasMoreElements();){
			ZipEntry ent = entries.nextElement();
			String name = ent.getName();
			ret.add(name);
		}
		return ret.toArray(new String[ret.size()]);
	}

	public class JarExtractor extends ZipExtractor{
		public JarExtractor(final File src) throws IOException{
			super(src);
		}
		public String[] list() throws IOException{
			Collection<String> ret = new Vector<String>();
			for(Enumeration<? extends ZipEntry> entries = new JarFile(this.src).entries(); entries.hasMoreElements();){
				ZipEntry ent = entries.nextElement();
				String name = ent.getName();
				ret.add(name);
			}
			return ret.toArray(new String[ret.size()]);
		}
	}
	public static void main(String[] args){
		try{
			compareArchives(args[0], args[1], null);

		}catch(IOException t){
			t.printStackTrace();
		}
	}
	/**barアーカイブ中のxpdlと単体xpdlを比較*/
	public static void compareXpdlToArchive(final String bar, final String xpdl) throws IOException{
		String archive = bar;
		ZipExtractor ext = new ZipExtractor(new File(archive));
		try{
			byte[] content = ext.extract(xpdl);
			System.out.println(xpdl + ":" + String.valueOf(content.length) + " bytes extracted.");
		}catch(Throwable t){t.printStackTrace();}
		String[] list = ext.list();
		for (String file : list) {
			System.out.println(file);
		}
	}
	/**アーカイブの内容を比較*/
	public static void compareArchives(final String file1, final String file2, Comparator<byte[]> comparater) throws IOException{
		boolean result = true;
		ZipExtractor f1=new ZipExtractor(new File(file1));
		ZipExtractor f2=new ZipExtractor(new File(file2));
		String[] list1 = f1.list();
		for(int i = 0; i < list1.length; i ++){
			ZipEntry cur1 = f1.getEntry(list1[i]);
			ZipEntry cur2 = f2.getEntry(list1[i]);
			byte[] b1 = f1.extract(list1[i]);
			byte[] b2 = f2.extract(list1[i]);
				System.out.println("\"" + file1 + "\"," + list1[i] + "," + 
					new DecimalFormat("###,###").format(cur1.getSize()) + "," + 
					new SimpleDateFormat("yyyy/MM/dd/HH:mm:ss").format(new Date(cur1.getTime())));
			if(cur2 == null){
				System.out.println("\"" + file2 + "\"" + " not contain " + list1[i]);
				result = false;
			}else{
				System.out.println("\"" + file2 + "," + list1[i] + "\"," + 
					new DecimalFormat("###,###").format(cur2.getSize()) + "," + 
					new SimpleDateFormat("yyyy/MM/dd/HH:mm:ss").format(new Date(cur2.getTime())));
	
				if(comparater == null){
					boolean same = Arrays.equals(b1,  b2);
					System.out.println(list1[i] + ": " + (same ? "matched*" : "unmatched*"));
					if(!same)result = same;
				}else{
					boolean same = (comparater.compare(b1,  b2) == 0);
					System.out.println(list1[i] + ": " + (same ? "matched" : "unmatched"));
					if(!same)result = same;
				}
			}
		}
		System.out.println((result ? "MATCHED: " : "UNMATCHED: ") + "\"" + file1 + "\"/\"" + file2 + "\"");
	}
	
}
