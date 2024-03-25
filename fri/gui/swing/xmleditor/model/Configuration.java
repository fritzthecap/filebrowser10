package fri.gui.swing.xmleditor.model;

import java.io.*;
import java.util.Properties;
import fri.gui.GuiConfig;
import fri.util.props.PropertyUtil;
import fri.util.text.TextUtil;

/**
	Obtains display options for XML node tree: types of displayed nodes.
	Holds parsing options like "do validation" and "expand entities".
	Loads defaults from persistence by a static initializer.

	@author Ritzberger Fritz
*/

public class Configuration implements
	Cloneable
{
	private static String VALIDATE = "validate";
	private static String EXPAND_ENTITIES = "expandEntities";
	private static String COMPLEX_MODE = "complexMode";
	private static String SHOW_COMMENTS = "showComments";
	private static String SHOW_PIS = "showPIs";
	private static String SHOW_PROLOG = "showProlog";
	private static String EXPAND_ALL_ON_OPEN = "expandAllOnOpen";
	private static String CREATE_ALL_TAGS_EMPTY = "createAllTagsEmpty";
	private static String MAP_TAGS = "mapTagsToSubstitutingAttributes";

	private static Configuration defaultConfiguration;

	public boolean validate;
	public boolean expandEntities;
	public boolean complexMode;
	public boolean showComments;
	public boolean showPIs;
	public boolean showProlog;
	public boolean expandAllOnOpen;
	public boolean createAllTagsEmpty;
	public boolean mapTags;


	/**
		Returns the defaults from persistence.
	*/
	public static Configuration getDefault()	{
		if (defaultConfiguration == null)	{
			Properties props = getProperties(getConfigurationPropertyFile());
			defaultConfiguration = new Configuration(
					PropertyUtil.checkProperty(VALIDATE, props),
					PropertyUtil.checkProperty(EXPAND_ENTITIES, props),
					PropertyUtil.checkProperty(COMPLEX_MODE, props),
					PropertyUtil.checkProperty(SHOW_COMMENTS, props, true),
					PropertyUtil.checkProperty(SHOW_PIS, props, true),
					PropertyUtil.checkProperty(SHOW_PROLOG, props, true),
					PropertyUtil.checkProperty(EXPAND_ALL_ON_OPEN, props),
					PropertyUtil.checkProperty(CREATE_ALL_TAGS_EMPTY, props),	// will not create attributes!
					PropertyUtil.checkProperty(MAP_TAGS, props)
					);
		}
		return defaultConfiguration;
	}

	/**
		Stores the passed defaults to persistence and to getDefault() return value.
	*/
	public static void setDefault(Configuration conf)	{
		defaultConfiguration = conf;

		Properties props = new Properties();
		props.put(VALIDATE, conf.validate ? "true" : "false");
		props.put(EXPAND_ENTITIES, conf.expandEntities ? "true" : "false");
		props.put(COMPLEX_MODE, conf.complexMode ? "true" : "false");
		props.put(SHOW_COMMENTS, conf.showComments ? "true" : "false");
		props.put(SHOW_PIS, conf.showPIs ? "true" : "false");
		props.put(SHOW_PROLOG, conf.showProlog ? "true" : "false");
		props.put(EXPAND_ALL_ON_OPEN, conf.expandAllOnOpen ? "true" : "false");
		props.put(CREATE_ALL_TAGS_EMPTY, conf.createAllTagsEmpty ? "true" : "false");
		props.put(MAP_TAGS, conf.mapTags ? "true" : "false");

		putProperties(getConfigurationPropertyFile(), props, "Default load options of XML editor");
	}

	/**
		Create Configuration with various flags
		@param validate parser should do validation when loading document
		@param expandEntities parser should expand entities when loading document
		@param complexMode treeview should show text in its element node (reduce tree nodes)
		@param complexMode treeview should show comments
	*/
	public Configuration(
		boolean validate,
		boolean expandEntities,
		boolean complexMode,
		boolean showComments,
		boolean showPIs,
		boolean showProlog,
		boolean expandAllOnOpen,
		boolean createAllTagsEmpty,
		boolean mapTags)
	{
		this.validate = validate;
		this.expandEntities = expandEntities;
		this.complexMode = complexMode;
		this.showComments = showComments;
		this.showPIs = showPIs;
		this.showProlog = showProlog;
		this.expandAllOnOpen = expandAllOnOpen;
		this.createAllTagsEmpty = createAllTagsEmpty;
		this.mapTags = mapTags;
	}


	/** Returns a new Configuration instance, identical with this one. */
	public Object clone()	{
		return new Configuration(
				validate,
				expandEntities,
				complexMode,
				showComments,
				showPIs,
				showProlog,
				expandAllOnOpen,
				createAllTagsEmpty,
				mapTags);
	}


	// Returns the file that contains the mapping of tag to attribute name.
	private static File getConfigurationPropertyFile()	{
		String name = GuiConfig.dir()+"xmleditor/";
		File f = new File(name);
		f.mkdirs();
		name = name+"Configuration.properties";
		return new File(name);
	}


	// Returns the file that contains the mapping of tag to attribute name.
	private File getTagMapPropertyFile(String rootTag)	{
		String name = GuiConfig.dir()+"xmleditor/tagmaps/";
		File f = new File(name);
		f.mkdirs();
		name = name+TextUtil.makeIdentifier(rootTag)+".properties";
		return new File(name);
	}

	/**
		Returns the hashtable that contains the mapping of tag to attribute names.
	*/
	public Properties getTagMapForRootTag(String rootTag)	{
		if (mapTags == false)
			return null;
		File f = getTagMapPropertyFile(rootTag);
		return getProperties(f);
	}

	private static Properties getProperties(File f)	{
		if (f.exists())	{
			InputStream is = null;
			try	{
				Properties props = new Properties();
				is = new FileInputStream(f);
				props.load(is);
				is.close();
				return props;
			}
			catch (Exception e)	{
				e.printStackTrace();
			}
			finally	{
				try	{ is.close(); } catch (Exception ex)	{}
			}
		}

		return null;
	}

	/**
		Store the passed properties as tag map to persistence.
	*/
	public Properties putTagMapForRootTag(String rootTag, Properties tagMap)	{
		File f = getTagMapPropertyFile(rootTag);
		return putProperties(f, tagMap, "Tag map for XML root tag \""+rootTag+"\"");
	}

	private static Properties putProperties(File f, Properties props, String comment)	{
		if (f.exists() && (props == null || props.size() <= 0))	{
			f.delete();
		}
		else
		if (props != null && props.size() > 0)	{
			OutputStream os = null;
			try	{
				os = new FileOutputStream(f);
				props.store(os, comment);
				return props;
			}
			catch (Exception e)	{
				e.printStackTrace();
			}
			finally	{
				try	{ os.close(); } catch (Exception ex)	{}
			}
		}

		return null;
	}

}