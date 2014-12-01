package de.latlon.deejump.plugin.manager;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class CatalogParser {

    private URL catalog;

    public CatalogParser(URL catalog) {
        this.catalog = catalog;
    }

    public List getExtensionList() {

        List tmpRemoteExtensions = new ArrayList(50);
        
//        System.out.println("found remote ext: " );
        
        try {
            DocumentBuilder docBuilder;
            docBuilder = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder();

            Document doc = docBuilder.parse(catalog.openStream());
            NodeList extNodes = doc.getDocumentElement().getElementsByTagName(
                    "Extension");
            for (int i = 0; i < extNodes.getLength(); i++) {
                Element ext = (Element) extNodes.item(i);

                String name = ext.getAttribute("name");
                String title = ext.getAttribute("title");
                String author = ext.getAttribute("author");
                String version = ext.getAttribute("version");
                String jumpVersion = ext.getAttribute("jumpVersion");
                String category = ext.getAttribute("category");

                String descrip = ext.getElementsByTagName("Description")
                        .item(0).getFirstChild().getNodeValue();

                NodeList resources = ((Element) ext.getElementsByTagName(
                        "ResourceList").item(0))
                        .getElementsByTagName("Resource");

                List resourcesList = new ArrayList(resources.getLength());
                for (int j = 0; j < resources.getLength(); j++) {
                    Element resource = (Element) resources.item(j);
                    resourcesList.add(resource.getAttribute("value"));
                }

                ExtensionWrapper catExtension = new ExtensionWrapper(
                        name, title, author, version, jumpVersion, category,
                        descrip, resourcesList);

                tmpRemoteExtensions.add(catExtension);
//                System.out.println( catExtension );

            }

            // DOMPrinter.printNode(System.out, doc);

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        return tmpRemoteExtensions;
    }

}
