/* ============================================================
 * JRobin : Pure java implementation of RRDTool's functionality
 * ============================================================
 *
 * Project Info:  http://www.jrobin.org
 * Project Lead:  Sasa Markovic (saxon@jrobin.org);
 *
 * (C) Copyright 2003, by Sasa Markovic.
 *
 * Developers:    Sasa Markovic (saxon@jrobin.org)
 *                Arne Vandamme (cobralord@jrobin.org)
 *
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation;
 * either version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * library; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 */

package org.jrobin.inspector;

import org.jrobin.core.*;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;

class RrdInspector extends JFrame {
	static final String TITLE = "RRD File Inspector";
	static final boolean SHOULD_FIX_ARCHIVED_VALUES = false;
	static final boolean SHOULD_CREATE_BACKUPS = true;

	static Dimension MAIN_TREE_SIZE = new Dimension(250, 400);
	static Dimension INFO_PANE_SIZE = new Dimension(450, 400);

	JTabbedPane tabbedPane = new JTabbedPane();
	private JTree mainTree = new JTree();
	private JTable generalTable = new JTable();
	private JTable datasourceTable = new JTable();
	private JTable archiveTable = new JTable();
	private JTable dataTable = new JTable();

	private InspectorModel inspectorModel = new InspectorModel();

	private String lastDirectory = null;

	private RrdInspector(String path) {
		super(TITLE);
		constructUI();
		showCentered();
		if(path == null) {
			selectFile();
		}
		else {
			loadFile(new File(path));
		}
	}

	private void showCentered() {
		pack();
		Toolkit t = Toolkit.getDefaultToolkit();
		Dimension screenSize = t.getScreenSize(), frameSize = getPreferredSize();
		double x = (screenSize.getWidth() - frameSize.getWidth()) / 2;
		double y = (screenSize.getHeight() - frameSize.getHeight()) / 2;
		setLocation((int) x, (int) y);
		setVisible(true);
	}

	private void constructUI() {
		JPanel content = (JPanel) getContentPane();
		content.setLayout(new BorderLayout());

		// WEST, tree pane
		JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new BorderLayout());
		JScrollPane treePane = new JScrollPane(mainTree);
		leftPanel.add(treePane);
		leftPanel.setPreferredSize(MAIN_TREE_SIZE);
		content.add(leftPanel, BorderLayout.WEST);
		mainTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		mainTree.addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				nodeChangedAction();
			}
		});
		mainTree.setModel(inspectorModel.getMainTreeModel());

		////////////////////////////////////////////
		// EAST, tabbed pane
		////////////////////////////////////////////

		// GENERAL TAB
		JScrollPane spGeneral = new JScrollPane(generalTable);
		spGeneral.setPreferredSize(INFO_PANE_SIZE);
		tabbedPane.add("General info", spGeneral);
		generalTable.setModel(inspectorModel.getGeneralTableModel());
		generalTable.getColumnModel().getColumn(0).setPreferredWidth(150);
		generalTable.getColumnModel().getColumn(0).setMaxWidth(150);

		// DATASOURCE TAB
		JScrollPane spDatasource = new JScrollPane(datasourceTable);
		spDatasource.setPreferredSize(INFO_PANE_SIZE);
		tabbedPane.add("Datasource info", spDatasource);
		datasourceTable.setModel(inspectorModel.getDatasourceTableModel());
		datasourceTable.getColumnModel().getColumn(0).setPreferredWidth(150);
		datasourceTable.getColumnModel().getColumn(0).setMaxWidth(150);

		// ARCHIVE TAB
		JScrollPane spArchive = new JScrollPane(archiveTable);
		archiveTable.setModel(inspectorModel.getArchiveTableModel());
		archiveTable.getColumnModel().getColumn(0).setPreferredWidth(150);
		archiveTable.getColumnModel().getColumn(0).setMaxWidth(150);
		spArchive.setPreferredSize(INFO_PANE_SIZE);
		tabbedPane.add("Archive info", spArchive);

		// DATA TAB
		JScrollPane spData = new JScrollPane(dataTable);
		dataTable.setModel(inspectorModel.getDataTableModel());
		dataTable.getColumnModel().getColumn(0).setPreferredWidth(100);
		dataTable.getColumnModel().getColumn(0).setMaxWidth(100);
		dataTable.getColumnModel().getColumn(1).setPreferredWidth(150);
		spData.setPreferredSize(INFO_PANE_SIZE);
		tabbedPane.add("Archive data", spData);

		content.add(tabbedPane, BorderLayout.CENTER);

		////////////////////////////////////////
		// MENU
		////////////////////////////////////////
		JMenuBar menuBar = new JMenuBar();
		// FILE
		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic(KeyEvent.VK_F);
		JMenuItem fileMenuItem = new JMenuItem("Open RRD file...", KeyEvent.VK_O);
		fileMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				selectFile();
			}
		});
		fileMenu.add(fileMenuItem);
		fileMenu.addSeparator();
		JMenuItem addDatasourceMenuItem = new JMenuItem("Add datasource...");
		addDatasourceMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addDatasource();
			}
		});
		fileMenu.add(addDatasourceMenuItem);
		JMenuItem editDatasourceMenuItem = new JMenuItem("Edit datasource...");
		editDatasourceMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				editDatasource();
			}
		});
		fileMenu.add(editDatasourceMenuItem);
		JMenuItem removeDatasourceMenuItem = new JMenuItem("Remove datasource");
		removeDatasourceMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				removeDatasource();
			}
		});
		fileMenu.add(removeDatasourceMenuItem);
		fileMenu.addSeparator();
		JMenuItem addArchiveMenuItem = new JMenuItem("Add archive...");
		addArchiveMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addArchive();
			}
		});
		fileMenu.add(addArchiveMenuItem);
		JMenuItem editArchiveMenuItem = new JMenuItem("Edit archive...");
		editArchiveMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				editArchive();
			}
		});
		fileMenu.add(editArchiveMenuItem);
		JMenuItem removeArchiveMenuItem = new JMenuItem("Remove archive...");
		removeArchiveMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				removeArchive();
			}
		});
		fileMenu.add(removeArchiveMenuItem);
		fileMenu.addSeparator();
		JMenuItem exitMenuItem = new JMenuItem("Exit", KeyEvent.VK_X);
		exitMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		fileMenu.add(exitMenuItem);
		menuBar.add(fileMenu);
		setJMenuBar(menuBar);

		// finalize UI
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});

	}

	private void nodeChangedAction() {
		RrdNode rrdNode = getSelectedRrdNode();
		if (rrdNode != null) {
			inspectorModel.selectModel(rrdNode.getDsIndex(), rrdNode.getArcIndex());
			if (rrdNode.getDsIndex() >= 0 && rrdNode.getArcIndex() >= 0) {
				// archive node
				if (tabbedPane.getSelectedIndex() < 2) {
					tabbedPane.setSelectedIndex(2);
				}
			} else if (rrdNode.getDsIndex() >= 0) {
				tabbedPane.setSelectedIndex(1);
			} else {
				tabbedPane.setSelectedIndex(0);
			}
		}
	}

	private RrdNode getSelectedRrdNode() {
		TreePath path = mainTree.getSelectionPath();
		if (path != null) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
			Object obj = node.getUserObject();
			if (obj instanceof RrdNode) {
				RrdNode rrdNode = (RrdNode) obj;
				return rrdNode;
			}
		}
		return null;
	}

	private void selectFile() {
		JFileChooser chooser = new JFileChooser(lastDirectory);
		FileFilter filter = new FileFilter() {
			public boolean accept(File f) {
				return f.isDirectory() ? true :
					f.getAbsolutePath().toLowerCase().endsWith(".rrd");
			}

			public String getDescription() {
				return "JRobin RRD files";
			}
		};
		chooser.setFileFilter(filter);
		int returnVal = chooser.showOpenDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File file = chooser.getSelectedFile();
			lastDirectory = file.getParent();
			//inspectorModel.setFile(file);
			//tabbedPane.setSelectedIndex(0);
			loadFile(file);
		}
	}

	private void loadFile(File file) {
		inspectorModel.setFile(file);
		tabbedPane.setSelectedIndex(0);
	}


	private void addDatasource() {
		if (!inspectorModel.isOk()) {
			Util.error(this, "Open a valid RRD file first.");
			return;
		}
		DsDef newDsDef = new EditDatasourceDialog(this, null).getDsDef();
		if (newDsDef != null) {
			// action
			RrdToolkit toolkit = RrdToolkit.getInstance();
			try {
				String sourcePath = inspectorModel.getFile().getCanonicalPath();
				toolkit.addDatasource(sourcePath, newDsDef, SHOULD_CREATE_BACKUPS);
				inspectorModel.refresh();
				tabbedPane.setSelectedIndex(0);
			} catch (IOException e) {
				Util.error(this, e.toString());
			} catch (RrdException e) {
				Util.error(this, e.toString());
			}
		}
	}

	private void addArchive() {
		if (!inspectorModel.isOk()) {
			Util.error(this, "Open a valid RRD file first.");
			return;
		}
		ArcDef newArcDef = new EditArchiveDialog(this, null).getArcDef();
		if (newArcDef != null) {
			// action
			RrdToolkit toolkit = RrdToolkit.getInstance();
			try {
				String sourcePath = inspectorModel.getFile().getCanonicalPath();
				toolkit.addArchive(sourcePath, newArcDef, SHOULD_CREATE_BACKUPS);
				inspectorModel.refresh();
				tabbedPane.setSelectedIndex(0);
			} catch (IOException e) {
				Util.error(this, e.toString());
			} catch (RrdException e) {
				Util.error(this, e.toString());
			}
		}
	}

	private void editDatasource() {
		if (!inspectorModel.isOk()) {
			Util.error(this, "Open a valid RRD file first.");
			return;
		}
		RrdNode rrdNode = getSelectedRrdNode();
		int dsIndex = -1;
		if(rrdNode == null || (dsIndex = rrdNode.getDsIndex()) < 0) {
			Util.error(this, "Select datasource first");
			return;
		}
		try {
			String sourcePath = inspectorModel.getFile().getCanonicalPath();
			RrdDb rrd = new RrdDb(sourcePath);
			DsDef dsDef = rrd.getRrdDef().getDsDefs()[dsIndex];
			rrd.close();
			DsDef newDsDef = new EditDatasourceDialog(this, dsDef).getDsDef();
			if(newDsDef != null) {
				// action!
				RrdToolkit toolkit = RrdToolkit.getInstance();
				toolkit.setDsHeartbeat(sourcePath, newDsDef.getDsName(),
					newDsDef.getHeartbeat());
				toolkit.setDsMinMaxValue(sourcePath, newDsDef.getDsName(),
					newDsDef.getMinValue(), newDsDef.getMaxValue(),	SHOULD_FIX_ARCHIVED_VALUES);
				inspectorModel.refresh();
				tabbedPane.setSelectedIndex(0);
			}
			rrd.close();
		} catch (IOException e) {
			Util.error(this, e.toString());
		} catch (RrdException e) {
			Util.error(this, e.toString());
		}
	}

	private void editArchive() {
		if (!inspectorModel.isOk()) {
			Util.error(this, "Open a valid RRD file first.");
			return;
		}
		RrdNode rrdNode = getSelectedRrdNode();
		int arcIndex = -1;
		if(rrdNode == null || (arcIndex = rrdNode.getArcIndex()) < 0) {
			Util.error(this, "Select archive first");
			return;
		}
		try {
			String sourcePath = inspectorModel.getFile().getCanonicalPath();
			RrdDb rrd = new RrdDb(sourcePath);
			ArcDef arcDef = rrd.getRrdDef().getArcDefs()[arcIndex];
			rrd.close();
			ArcDef newArcDef = new EditArchiveDialog(this, arcDef).getArcDef();
			if(newArcDef != null) {
				// action!
				RrdToolkit toolkit = RrdToolkit.getInstance();
				// fix X-files factor
				toolkit.setArcXff(sourcePath, newArcDef.getConsolFun(),
					newArcDef.getSteps(), newArcDef.getXff());
                // fix archive size
				toolkit.resizeArchive(sourcePath, newArcDef.getConsolFun(),
					newArcDef.getSteps(), newArcDef.getRows(), SHOULD_CREATE_BACKUPS);
				inspectorModel.refresh();
				tabbedPane.setSelectedIndex(0);
			}
			rrd.close();
		} catch (IOException e) {
			Util.error(this, e.toString());
		} catch (RrdException e) {
			Util.error(this, e.toString());
		}
	}

	private void removeDatasource() {
		if (!inspectorModel.isOk()) {
			Util.error(this, "Open a valid RRD file first.");
			return;
		}
		RrdNode rrdNode = getSelectedRrdNode();
		int dsIndex = -1;
		if(rrdNode == null || (dsIndex = rrdNode.getDsIndex()) < 0) {
			Util.error(this, "Select datasource first");
			return;
		}
		try {
			String sourcePath = inspectorModel.getFile().getCanonicalPath();
			RrdDb rrd = new RrdDb(sourcePath);
			String dsName = rrd.getRrdDef().getDsDefs()[dsIndex].getDsName();
			rrd.close();
			RrdToolkit toolkit = RrdToolkit.getInstance();
			toolkit.removeDatasource(sourcePath, dsName, SHOULD_CREATE_BACKUPS);
			inspectorModel.refresh();
			tabbedPane.setSelectedIndex(0);
		} catch (IOException e) {
			Util.error(this, e.toString());
		} catch (RrdException e) {
			Util.error(this, e.toString());
		}
	}

	private void removeArchive() {
		if (!inspectorModel.isOk()) {
			Util.error(this, "Open a valid RRD file first.");
			return;
		}
		RrdNode rrdNode = getSelectedRrdNode();
		int arcIndex = -1;
		if(rrdNode == null || (arcIndex = rrdNode.getArcIndex()) < 0) {
			Util.error(this, "Select archive first");
			return;
		}
		try {
			String sourcePath = inspectorModel.getFile().getCanonicalPath();
			RrdDb rrd = new RrdDb(sourcePath);
			ArcDef arcDef = rrd.getRrdDef().getArcDefs()[arcIndex];
			String consolFun = arcDef.getConsolFun();
			int steps = arcDef.getSteps();
			rrd.close();
			RrdToolkit toolkit = RrdToolkit.getInstance();
			toolkit.removeArchive(sourcePath, consolFun, steps, SHOULD_CREATE_BACKUPS);
			inspectorModel.refresh();
			tabbedPane.setSelectedIndex(0);
		} catch (IOException e) {
			Util.error(this, e.toString());
		} catch (RrdException e) {
			Util.error(this, e.toString());
		}
	}

	private static void printUsageAndExit() {
		System.err.println("usage: " + RrdInspector.class.getName() + " [<filename>]");
		System.exit(1);
	}

	public static void main(String[] args) {
		if (args.length > 1) {
			printUsageAndExit();
		}
		String path = (args.length == 1)? args[0]: null;
		new RrdInspector(path);
	}
}
