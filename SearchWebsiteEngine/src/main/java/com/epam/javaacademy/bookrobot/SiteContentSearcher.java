package com.epam.javaacademy.bookrobot;

import static com.epam.javaacademy.bookrobot.SearchingLogger.logForSearchingStart;
import static com.epam.javaacademy.bookrobot.SearchingLogger.logFoundBooksNumber;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class SiteContentSearcher 
{
	SearchingLogger logger = new SearchingLogger();

	public boolean isMarkerFound(String htmlAdress, String marker){
		String url = htmlAdress;
		Document document = null;
		try {
			document = Jsoup.connect(url).followRedirects(false).timeout(60000).get();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Elements elements = document.select(marker);
		if(elements.size()>0) return true;
		return false;
	}
	
	/**
	 * Creating a Map<String, String> where key i a book's name and value is a url of the book store's site.
	 * 
	 * @param CompleteListOfURLs urlsMap
	 * @return Map
	 * @throws IOException
	 */
	public Map<String, String> searchAllSitesForBooks(CompleteListOfURLs urlsMap) throws IOException{
		Map<String, String> booksMap = new HashMap<String, String>();
		Map <URL, ArrayList <String> > siteMap = urlsMap.createMap();

		logForSearchingStart();
		
		for(Entry<URL, ArrayList<String>> entry : siteMap.entrySet()){
			ArrayList<String> storesList = entry.getValue();
			HashSet<String> storesSet = convertToSet(storesList);
			for(String adress : storesSet){
				ArrayList<String> foundBooksList = searchInSite(adress);
				
				System.out.println(adress);
				System.out.println(foundBooksList.size());
				
				putBooksIntoMap(booksMap, adress, foundBooksList);
			}
		}
		
		BooksToFileWriter writer = new BooksToFileWriter();
		writer.write(booksMap);
		logFoundBooksNumber(booksMap.size());
		return booksMap;
	}

	private void putBooksIntoMap(Map<String, String> booksMap, String adress, ArrayList<String> foundBooksList) {
		for(String s : foundBooksList){
			booksMap.put(s, adress);
		}
	}
	
	/**
	 * Converting List to Set
	 * 
	 * @param List list)
	 * @return HashSet
	 */
	private HashSet<String> convertToSet(List<String> list){
			HashSet<String> set = new HashSet<>();
				for(String s : list) set.add(s);
			return set;
	
	}
	
	/**
	 * Creating an ArrayList of names of books found in the site.
	 * @param String url
	 * @return ArrayList
	 * @throws IOException
	 */
	protected ArrayList<String> searchInSite(String url) {
		ArrayList<String> list = new ArrayList<>();
			try {
				list.addAll(searchOnNexto(url));
				list.addAll(searchOnPublio(url));
				list.addAll(searchOnVirtualo(url));
			} catch (Exception e) {			
				SearchingLogger.logException(url, e.getMessage(), e.getClass());				
			}
		return list;
	}

	ArrayList<String> searchOnPublio(String url) throws IOException{

		String marker = "div[class=product-tile-price-wrapper]";
		Document document = Jsoup.connect(url).followRedirects(false).timeout(60000).get();
        Elements divElements = document.select(marker);
        
        ArrayList<String> titleList = new ArrayList<>();
        
        for(Element divElement : divElements){
        	Elements priceElements = divElement.select("ins[class=product-tile-price]");
        	String price = priceElements.html();
        	addTitlesToList(titleList, divElement, price);
        }
        
        for (String i : titleList){
        	System.out.println(i);
        }
        
        return titleList; 
	}
	
	ArrayList<String> searchOnNexto(String url) throws IOException {
		
		String marker = "a[class=title]";
		String priceMarker = "strong[class=nprice]";
		ArrayList<String> titleList = new ArrayList<>();
		
		Document document = Jsoup.connect(url).followRedirects(false).timeout(60000).get();
        Elements aElements = document.select(marker);
        Elements priceElements = document.select(priceMarker);
        
        Element [] prices = new Element[priceElements.size()]; 
        priceElements.toArray(prices);
        
        int i = 0;
        for(Element aElement : aElements){
        	Pattern pricePattern = Pattern.compile("0[,.]00.*");
        	
        	Matcher m = pricePattern.matcher(prices[i].html());
        	
        	if(m.matches()){
        		titleList.add(aElement.html());
        	}
        }
		return titleList;
	}
	
	ArrayList<String> searchOnVirtualo(String url) throws IOException {

		String marker = "div[class=content]";
		String priceMarker = "div[class=price]";
		ArrayList<String> titleList = new ArrayList<>();
		
		Document document = Jsoup.connect(url).followRedirects(false).timeout(60000).get();
        Elements divElements = document.select(marker);
        	
        	for(Element divElement : divElements){
        		Elements priceElements = divElement.select(priceMarker);
        		Element priceElement = priceElements.first();

        		if(priceElement != null) {
        			String price = priceElement.html();
        			
        			Matcher priceMatcher = matchPrice(price);
        			
        			if(priceMatcher.matches()){
        				Elements titleElements = divElement.select("div[class=title]");
        				String title = titleElements.html().split("\n")[0];
        				titleList.add(title);
	            	
        			}
        		}
        	}
        
        return titleList;
	}
   
	private void addTitlesToList(ArrayList<String> titleList, Element element, String price) {
		Matcher m = matchPrice(price);

		if(m.matches()){
			Elements titleElements = element.select("a[title]");
			String title = titleElements.attr("title");
			titleList.add(title);
		}
	}
	
	private Matcher matchPrice(String price) {
		Pattern pricePattern = Pattern.compile("0[,.]00.*");
		Matcher m = pricePattern.matcher(price);
		return m;
	}

	
}
