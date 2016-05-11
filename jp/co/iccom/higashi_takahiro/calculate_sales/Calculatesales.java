package jp.co.iccom.higashi_takahiro.calculate_sales;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Calculatesales {
	
	// 支店定義ファイルMap,商品定義ファイルMap
	static HashMap<String, String> branchMap = new HashMap<String, String>();
	static HashMap<String, String> commodityMap = new HashMap<String, String>();
	// 売り上げMap(支店コード・商品コードをkey、金額をvalueに格納)
	static HashMap<String, Long> branchRcdMap = new HashMap<String, Long>();
	static HashMap<String, Long> commodityRcdMap = new HashMap<String, Long>();
	
	public static void main(String[]args) throws Exception{
		if(args.length == 0 || args.length > 1  || !new File(args[0]).exists()) {
			System.out.println("予期せぬエラーが発生しました");
			return;
		}
		
		File branch = new File(args[0] + File.separator + "branch.lst");
		File commodity = new File(args[0] + File.separator + "commodity.lst");
			
		//支店定義ファイル読み込み、フォーマットチェック、存在判定
		if(!readDefinitionFile("支店",branch,"^\\d{3}$",branchMap,branchRcdMap)){
			return;
		}
		//商品定義ファイル読み込み、フォーマットチェック、存在判定
		if(!readDefinitionFile("商品",commodity,"^\\w{8}$",commodityMap,commodityRcdMap)){
			return;
		}
	
		// ディレクトリのファイル一覧を取得
		String path = args[0];
		File folder = new File(path);
		File[] folderList = folder.listFiles(); // dir内のファイルを配列に格納

		// 売上ファイル格納
		ArrayList<String> rcdFolder = new ArrayList<String>();
		ArrayList<Integer> rcdNo = new ArrayList<Integer>();
		
		// 売上ファイル抽出
		earingsExtraction(rcdFolder,folderList,"^\\d{8}.rcd$",rcdNo);
						
		// 連番処理
		if(!numberCheck(rcdNo, folderList)){
			return;
		}
							
		// 売り上げファイル読み込み
		ArrayList<String> rcdEarings = new ArrayList<String>();
		
		for(int i = 0; i < rcdFolder.size(); i++) {
			if(!readRcd(folder, rcdFolder.get(i), rcdEarings )){
				return;
			}
			String branchCode = rcdEarings.get(0);
			String commodityCode = rcdEarings.get(1);
			long amount = Long.parseLong(rcdEarings.get(2));
			
			if(!amountCheck ("支店", rcdFolder.get(i), amount, branchRcdMap, branchCode)){
				return;
			}
    		  
			if(!amountCheck ("商品", rcdFolder.get(i), amount, commodityRcdMap, commodityCode)){
				return;
			}
			rcdEarings = new ArrayList<String>();
		}
		
		List<Map.Entry<String,Long>> sortBranch = earingsSort(branchRcdMap);
		List<Map.Entry<String,Long>> sortCommodity = earingsSort(commodityRcdMap);

		// 支店別集計ファイル作成
		
		File branchout = new File(args[0] + File.separator + "branch.out");
		File commodityout = new File(args[0] + File.separator + "commodity.out");
		
		// 集計ファイルの出力
				if(!writeRcd(branchout, sortBranch, branchMap)) {
					return;
				}
				if(!writeRcd(commodityout, sortCommodity, commodityMap)) {
					return;
				}
	}
	
	//「メソッド」定義ファイル読み込み、存在判定、フォーマットチェック
	private static  boolean readDefinitionFile(String definition,File fileName,String format,
			HashMap<String, String> definitionMap,HashMap<String, Long> definitionRcdMap) {
		
		if(!fileName.exists()){
			System.out.println(definition + "定義ファイルが存在しません");
			return false;
		}
		
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(fileName));
			String line;
			while ((line = br.readLine()) != null) {
				String[] words =line.split(",");
				if (words.length != 2 || !words[0].matches(format)) {
					System.out.println(definition + "定義ファイルのフォーマットが不正です");
					return false;
				}
				definitionMap.put(words[0],words[1]);// [0]支店コード　[1]支店名
				definitionRcdMap.put(words[0],0L);
			}
		} catch (Exception e) {
				System.out.println("予期せぬエラーが発生しました");
				return false;
		} finally {
				try {
					br.close();
				} catch (Exception e) {
					System.out.println("予期せぬエラーが発生しました");
					return false;
				}
		}
		return true;
	}

    //「メソッド」売上ファイル読み込み、フォーマットチェック
	private static  boolean readRcd(File folder, String fileName, ArrayList<String> rcdEarings) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(folder + File.separator + fileName));
			String readLine;
			while((readLine = br.readLine()) != null){
				rcdEarings.add(readLine); //get(0)001,get(1)SFT0001,get(2)10000
			}	
			// 売り上げファイルの中身が４行以上ある場合フォーマットエラー
			if((rcdEarings.size() != 3)) {
				System.out.println(fileName + "のフォーマットが不正です");
				return false;
			}
		}catch(Exception e){
			System.out.println("予期せぬエラーが発生しました");
			return false;
		}finally{
			try {
				br.close();
				} catch (Exception e) {
					System.out.println("予期せぬエラーが発生しました");
					return false;
				}
		}
	return true;
	}

    //「メソッド」売上ファイル集計、コードエラー処理
	private static  boolean amountCheck(String definition, String fileName,long amount,
			HashMap<String, Long> amountRcdMap, String code) {
		
		// 定義ファイル、コードエラー処理
		if (!amountRcdMap.containsKey(code)) {
			System.out.println(fileName + "の"+ definition + "コードが不正です");
			return false;
		}
				
		// 定義ファイル別集計
		if(amountRcdMap.get(code) != null) {
			long mount = amountRcdMap.get(code);
			long newMount = mount + amount;
			amountRcdMap.put(code,newMount);
			if(String.valueOf(newMount).length() > 10){
				System.out.println("合計金額が10桁を超えました");
				return false;
			}
		}          
		return true;
	}
		//「メソッド」売上ファイル読み込み

//「メソッド」商品別集計ファイルの作成

	private static boolean writeRcd(File fileName, List<Map.Entry<String, Long>> sortDefinition, 
			HashMap<String, String> definitionMap){
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(fileName));
			fileName.createNewFile();
			
			for (Entry<String, Long> g : sortDefinition) {
				bw.write(g.getKey() + "," + definitionMap.get(g.getKey()) +","+ g.getValue());
				bw.newLine();
			}
		} catch (Exception e) {
			System.out.println("予期せぬエラーが発生しました");
			return false;
		} finally {
			try {
				bw.close();
			} catch (Exception e) {
				System.out.println("予期せぬエラーが発生しました");
				return false;
			}	
			
		}
		return true;
	}
	
//「メソッド」ソート降順
	private static List<Entry<String,Long>> earingsSort(Map<String, Long> definitionRcdMap) {
	List<Map.Entry<String, Long>> sortDefinition 
	= new ArrayList<Map.Entry<String,Long>>(definitionRcdMap.entrySet());
	Collections.sort(sortDefinition, new Comparator<Map.Entry<String,Long>>() {
		@Override
		public int compare(Entry<String,Long> entry1, Entry<String,Long> entry2) {
			return (entry2.getValue()).compareTo(entry1.getValue());
		}
	});
	return sortDefinition;
	}
	

	//「メソッド」連番処理
	private static boolean numberCheck(ArrayList<Integer> rcdNomber, File[] fileList){
		for (int i = 0; i < rcdNomber.size(); i++ ) {
			if(rcdNomber.get(i) != i + 1 || fileList[i].isDirectory()){
				System.out.println("売上ファイル名が連番になっていません");
				return false;
			}
		}
		return true;
	}

	//「メソッド」売上抽出
	static void earingsExtraction(ArrayList<String> rcdFile, File[] fileName,String searchFormat,
			ArrayList<Integer> rcdNomber){
		
		// 売上ファイル抽出
		for (File value : fileName) { 
			File inputFile = value;
			if (inputFile.getName().matches(searchFormat)) { // 数字8桁、rcdファイル検索
				if(inputFile.isFile()){
					rcdFile.add(inputFile.getName());
				}
				
				//フォルダ除去
				String[] rcdSplit = inputFile.getName().split("\\.");
				rcdNomber.add( new Integer(rcdSplit[0]).intValue());		
			}
		}
		return;
	}
}