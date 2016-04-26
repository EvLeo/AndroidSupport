package com.hyena.framework.samples.parser;

import android.text.TextUtils;

import com.hyena.framework.samples.parser.action.MapAction;
import com.hyena.framework.samples.parser.action.MapActionAlpha;
import com.hyena.framework.samples.parser.action.MapActionRotate;
import com.hyena.framework.samples.parser.action.MapFrame;
import com.hyena.framework.samples.parser.action.MapActionFrame;
import com.hyena.framework.samples.parser.action.MapActionScale;
import com.hyena.framework.samples.parser.action.MapActionSequence;
import com.hyena.framework.samples.parser.action.MapActionTranslate;
import com.hyena.framework.samples.parser.node.MapNodeLine;
import com.hyena.framework.samples.parser.node.MapNodeLayer;
import com.hyena.framework.samples.parser.node.MapNode;
import com.hyena.framework.samples.parser.node.MapNodeSprite;
import com.hyena.framework.samples.parser.node.MapNodeText;
import com.hyena.framework.samples.parser.utils.XMLUtils;
import com.hyena.framework.utils.MathUtils;
import com.hyena.framework.utils.UIUtils;

import org.apache.http.protocol.HTTP;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by yangzc on 16/4/21.
 */
public class DefaultMapParser implements MapParser {

    @Override
    public CMap parse(String xml, int screenWidth, int screenHeight) {
        try {
            CMap map = new CMap();
            DocumentBuilder builder = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(xml.getBytes(HTTP.UTF_8)));
            Element rootElement = document.getDocumentElement();

            //初始化背景
            NodeList backGroundList = rootElement.getElementsByTagName("background");
            String backGround = "";
            for (int i = 0; i < backGroundList.getLength(); i++) {
                Node backGroundNode = backGroundList.item(i);
                backGround = XMLUtils.getAttributeValue(backGroundNode, "src");
            }
            map.mBackGround = backGround;

            NodeList layerList = rootElement.getElementsByTagName("layer");
            for (int i = 0; i < layerList.getLength(); i++) {
                Node layer = layerList.item(i);
                MapNodeLayer mapLayer = parseLayer(layer, screenWidth, screenHeight);
                map.addLayer(mapLayer);
            }
            return map;
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private MapNodeLayer parseLayer(Node layer, int screenWidth, int screenHeight) {
        MapNodeLayer mapLayer = new MapNodeLayer();
        mapLayer.setId(XMLUtils.getAttributeValue(layer, "id"));
        mapLayer.setZIndex(MathUtils.valueOfInt(XMLUtils.getAttributeValue(layer, "zindex")));
        String depth = XMLUtils.getAttributeValue(layer, "depth");
        if (!TextUtils.isEmpty(depth)) {
            mapLayer.setDepth(MathUtils.valueOfFloat(depth));
        } else {
            mapLayer.setDepth(1);
        }
        NodeList elementNode = layer.getChildNodes();
        for (int i = 0; i < elementNode.getLength(); i++) {
            Node element = elementNode.item(i);
            String nodeName = element.getNodeName();
            MapNode mapNode = null;
            if ("node".equals(nodeName)) {
                mapNode = parseSprite(element, screenWidth, screenHeight);
            } else if ("text".equals(nodeName)) {
                mapNode = parseText(element, screenWidth, screenHeight);
            } else if ("line".equals(nodeName)) {
                mapNode = parseLine(element);
            }
            if (mapNode != null) {
                //update x, y, tag
                updateMapNode(mapNode, element, screenWidth, screenHeight);
                //update actions
                updateActions(mapNode, element);

                mapLayer.addNode(mapNode);
            }
        }
        return mapLayer;
    }

    private void updateMapNode(MapNode mapNode, Node node, int screenWidth, int screenHeight) {
        mapNode.setX(getNumber(XMLUtils.getAttributeValue(node, "x"), screenWidth, screenHeight));
        mapNode.setY(getNumber(XMLUtils.getAttributeValue(node, "y"), screenWidth, screenHeight));
        mapNode.setZIndex(MathUtils.valueOfInt(XMLUtils.getAttributeValue(node, "zindex")));
        mapNode.setTag(XMLUtils.getAttributeValue(node, "tag"));

        String anchorX = XMLUtils.getAttributeValue(node, "anchorX");
        if (!TextUtils.isEmpty(anchorX)) {
            mapNode.setAnchorX(MathUtils.valueOfFloat(anchorX));
        }

        String anchorY = XMLUtils.getAttributeValue(node, "anchorY");
        if (!TextUtils.isEmpty(anchorY)) {
            mapNode.setAnchorY(MathUtils.valueOfFloat(anchorY));
        }
    }

    private void updateActions(MapNode mapNode, Node node) {
        NodeList nodeList = node.getChildNodes();
        if (nodeList != null && nodeList.getLength() > 0) {
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node child = nodeList.item(i);
                MapAction action = parseActions(child);
                if (action != null) {
                    mapNode.addAction(action);
                }
            }
        }
    }

    private MapAction parseActions(Node node){
        MapAction action = null;
        String type = XMLUtils.getAttributeValue(node, "type");
        if ("scale".equals(type)) {
            action = parseScaleAction(node);
        } else if ("translate".equals(type)) {
            action = parseTranslateAction(node);
        } else if ("alpha".equals(type)) {
            action = parseAlphaAction(node);
        } else if ("frame".equals(type)) {
            action = parseFrameAction(node);
        } else if ("sequence".equals(type)) {
            action = parseSequenceAction(node);
        } else if ("rotate".equals(type)) {
            action = parseRotateAction(node);
        }
        return action;
    }

    private MapActionRotate parseRotateAction(Node node){
        int duration = MathUtils.valueOfInt(XMLUtils.getAttributeValue(node, "duration"));
//        int repeat = MathUtils.valueOfInt(XMLUtils.getAttributeValue(node, "repeat"));
        String repeatStr = XMLUtils.getAttributeValue(node, "repeat");
        int repeat = 1;
        if (!TextUtils.isEmpty(repeatStr)) {
            repeat = MathUtils.valueOfInt(repeatStr);
        }
        MapActionRotate action = new MapActionRotate(duration, repeat);
        action.mFrom = MathUtils.valueOfInt(XMLUtils.getAttributeValue(node, "from"));
        action.mDegree = MathUtils.valueOfInt(XMLUtils.getAttributeValue(node, "degree"));
        return action;
    }

    private MapActionSequence parseSequenceAction(Node node) {
        int duration = MathUtils.valueOfInt(XMLUtils.getAttributeValue(node, "duration"));
//        int repeat = MathUtils.valueOfInt(XMLUtils.getAttributeValue(node, "repeat"));
        String repeatStr = XMLUtils.getAttributeValue(node, "repeat");
        int repeat = 1;
        if (!TextUtils.isEmpty(repeatStr)) {
            repeat = MathUtils.valueOfInt(repeatStr);
        }
        MapActionSequence action = new MapActionSequence(duration, repeat);

        NodeList actionList = node.getChildNodes();
        if (actionList != null && actionList.getLength() > 0) {
            for (int i = 0; i < actionList.getLength(); i++) {
                Node actionNode = actionList.item(i);
                MapAction subAction = parseActions(actionNode);
                if (subAction != null) {
                    action.addAction(subAction);
                }
            }
        }

        return action;
    }

    private MapActionFrame parseFrameAction(Node node) {
        int duration = MathUtils.valueOfInt(XMLUtils.getAttributeValue(node, "duration"));
//        int repeat = MathUtils.valueOfInt(XMLUtils.getAttributeValue(node, "repeat"));
        String repeatStr = XMLUtils.getAttributeValue(node, "repeat");
        int repeat = 1;
        if (!TextUtils.isEmpty(repeatStr)) {
            repeat = MathUtils.valueOfInt(repeatStr);
        }
        MapActionFrame action = new MapActionFrame(duration, repeat);

        NodeList frameList = node.getChildNodes();
        if (frameList != null && frameList.getLength() > 0) {
            for (int i = 0; i < frameList.getLength(); i++) {
                Node frameNode = frameList.item(i);

                MapFrame frame = new MapFrame(MathUtils.valueOfInt(
                        XMLUtils.getAttributeValue(frameNode, "duration")),
                        MathUtils.valueOfInt(XMLUtils.getAttributeValue(frameNode, "repeat")));
                frame.mSrc = XMLUtils.getAttributeValue(frameNode, "src");
                action.addFrame(frame);
            }
        }
        return action;
    }

    private MapActionAlpha parseAlphaAction(Node node) {
        int duration = MathUtils.valueOfInt(XMLUtils.getAttributeValue(node, "duration"));
//        int repeat = MathUtils.valueOfInt(XMLUtils.getAttributeValue(node, "repeat"));
        String repeatStr = XMLUtils.getAttributeValue(node, "repeat");
        int repeat = 1;
        if (!TextUtils.isEmpty(repeatStr)) {
            repeat = MathUtils.valueOfInt(repeatStr);
        }
        MapActionAlpha action = new MapActionAlpha(duration, repeat);
        action.mFrom = MathUtils.valueOfInt(XMLUtils.getAttributeValue(node, "from"));
        action.mTo = MathUtils.valueOfInt(XMLUtils.getAttributeValue(node, "to"));
        return action;
    }

    private MapActionTranslate parseTranslateAction(Node node) {
        int duration = MathUtils.valueOfInt(XMLUtils.getAttributeValue(node, "duration"));
        String repeatStr = XMLUtils.getAttributeValue(node, "repeat");
        int repeat = 1;
        if (!TextUtils.isEmpty(repeatStr)) {
            repeat = MathUtils.valueOfInt(repeatStr);
        }
        MapActionTranslate action = new MapActionTranslate(duration, repeat);
        action.mToX = MathUtils.valueOfInt(XMLUtils.getAttributeValue(node, "toX"));
        action.mToY = MathUtils.valueOfInt(XMLUtils.getAttributeValue(node, "toY"));
        return action;
    }

    private MapActionScale parseScaleAction(Node node) {
        int duration = MathUtils.valueOfInt(XMLUtils.getAttributeValue(node, "duration"));
        String repeatStr = XMLUtils.getAttributeValue(node, "repeat");
        int repeat = 1;
        if (!TextUtils.isEmpty(repeatStr)) {
            repeat = MathUtils.valueOfInt(repeatStr);
        }
        MapActionScale action = new MapActionScale(duration, repeat);
        action.mFrom = MathUtils.valueOfFloat(XMLUtils.getAttributeValue(node, "from"));
        action.mTo = MathUtils.valueOfFloat(XMLUtils.getAttributeValue(node, "to"));
        return action;
    }

    private MapNodeSprite parseSprite(Node node, int screenWidth, int screenHeight) {
        String id = XMLUtils.getAttributeValue(node, "id");
        String width = XMLUtils.getAttributeValue(node, "width");
        String height = XMLUtils.getAttributeValue(node, "height");
        MapNodeSprite sprite = new MapNodeSprite(id,
                getNumber(width, screenWidth, screenHeight),
                getNumber(height, screenWidth, screenHeight));
        sprite.mSrc = XMLUtils.getAttributeValue(node, "src");
        return sprite;
    }

    private MapNodeText parseText(Node node, int screenWidth, int screenHeight) {
        String id = XMLUtils.getAttributeValue(node, "id");
        String width = XMLUtils.getAttributeValue(node, "width");
        String height = XMLUtils.getAttributeValue(node, "height");
        MapNodeText text = new MapNodeText(id,
                getNumber(width, screenWidth, screenHeight),
                getNumber(height, screenWidth, screenHeight));
        text.mColor = XMLUtils.getAttributeValue(node, "color");
        text.mPressColor = XMLUtils.getAttributeValue(node, "pressed");
        text.mFontSize = MathUtils.valueOfInt(XMLUtils.getAttributeValue(node, "fontSize"));
        text.mText = XMLUtils.getAttributeValue(node, "text");
        text.mAlign = XMLUtils.getAttributeValue(node, "align");
        return text;
    }

    private MapNodeLine parseLine(Node node) {
        String id = XMLUtils.getAttributeValue(node, "id");
        MapNodeLine line = new MapNodeLine(id, 0, 0);
        line.mFromId = XMLUtils.getAttributeValue(node, "from");
        line.mToId = XMLUtils.getAttributeValue(node, "to");
        line.mStyle = XMLUtils.getAttributeValue(node, "style");
        line.mColor = XMLUtils.getAttributeValue(node, "color");
        return line;
    }

    private int getNumber(String value, int screenWidth, int screenHeight) {
        if (TextUtils.isEmpty(value))
            return 0;
        try {
            if (value.startsWith("func")) {
                //取得公式部分
                String eval = value.substring(5);
                eval = eval.substring(0, eval.indexOf(")"));
                eval = eval.replaceAll("HEIGHT", screenHeight + "");
                eval = eval.replaceAll("WIDTH", screenWidth + "");
                int result;
                try {
                    result = Integer.valueOf(eval);
                } catch (Exception e) {
                    result = MathUtils.eval(eval);
                }
                return UIUtils.dip2px(result);
            }
            return UIUtils.dip2px(Integer.valueOf(value));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}
