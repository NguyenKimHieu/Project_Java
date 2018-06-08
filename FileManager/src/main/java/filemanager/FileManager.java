
package filemanager;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Container;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.*;
import java.awt.image.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.table.*;
import javax.swing.filechooser.FileSystemView;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import javax.imageio.ImageIO;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;

import java.io.*;
import java.nio.channels.FileChannel;


import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;




public class FileManager {

    /** Tiêu đề Ứng dụng */
    public static final String APP_TITLE = "File Manager";

    private static void copyFolder(File srcFile, File destFile) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /** Dùng để mở Files */
    private Desktop desktop;
    
    /** Hiển thị Icon và Tên files. */
    private FileSystemView fileSystemView;

    /** Các Files đang chọn hiện tại. */
    private File currentFile;

    /** Main GUI container */
    private JPanel gui;

    /** Hiển thị dạng cây.  */
    private JTree tree;
    private DefaultTreeModel treeModel;

    /** Danh sách các thư mục */
    private JTable table;
    private JProgressBar progressBar;
    
    /** Danh sách Files. */
    private FileTableModel fileTableModel;
    private ListSelectionListener listSelectionListener;
    private boolean cellSizesSet = false;
    private int rowIconPadding = 6;

    /* File controls. Khai báo các button cần dùng. */
    private JButton openFile;
    private JButton deleteFile;
    private JButton newFile;
    private JButton copyFile;
    private JButton pasteFile;
    private JButton cutFile;
    
    
    /* File details. Nội dung cần có trong Table */
    private JLabel fileName;
    private JTextField path;
    private JLabel date;
    private JLabel size;
    private JCheckBox readable;
    private JCheckBox writable;
    private JCheckBox executable;
    private JRadioButton isDirectory;
    private JRadioButton isFile;

    /* GUI options/containers for new File/Directory creation.  . */
    private JPanel newFilePanel;
    private JRadioButton newTypeFile;
    private JTextField name;
    
    /*Khai báo đường dẫn Source and Dest khi Copy*/
    private File Source;
    private File Dest;
    
    /*Biến cờ để xác định Paste cảu thao tác Copy hay Cut*/
    //True= Copy ---- False=Cut---
    private boolean Flag_Paste;
    
    
    /*CHẠY TRONG MAIN*/
    /*Tạo GUI */
    public Container getGui() 
    {
        if (gui==null) 
        {
            /* Tạo Gui mới.*/
            gui = new JPanel(new BorderLayout(3,3));
            gui.setBorder(new EmptyBorder(5,5,5,5));

            fileSystemView = FileSystemView.getFileSystemView();
            desktop = Desktop.getDesktop();
            
            
            //Nội dung chi tiết của File/Folder đang chọn!
            JPanel detailView = new JPanel(new BorderLayout(3,3));
            //fileTableModel = new FileTableModel();

            table = new JTable();
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setAutoCreateRowSorter(true);
            table.setShowVerticalLines(false);

            listSelectionListener = new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent lse) {
                    int row = table.getSelectionModel().getLeadSelectionIndex();
                    setFileDetails( ((FileTableModel)table.getModel()).getFile(row) );
                }
            };
            table.getSelectionModel().addListSelectionListener(listSelectionListener);
            JScrollPane tableScroll = new JScrollPane(table);
            Dimension d = tableScroll.getPreferredSize();
            tableScroll.setPreferredSize(new Dimension((int)d.getWidth(), (int)d.getHeight()/2));
            detailView.add(tableScroll, BorderLayout.CENTER);

            // the File tree
            DefaultMutableTreeNode root = new DefaultMutableTreeNode();
            treeModel = new DefaultTreeModel(root);

            TreeSelectionListener treeSelectionListener = new TreeSelectionListener() {
                public void valueChanged(TreeSelectionEvent tse){
                    DefaultMutableTreeNode node =
                        (DefaultMutableTreeNode)tse.getPath().getLastPathComponent();
                    showChildren(node);
                    setFileDetails((File)node.getUserObject());
                }
            };

            // Hiển thị hệ thông tập tin gốc
            File[] roots = fileSystemView.getRoots();
            for (File fileSystemRoot : roots) {
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(fileSystemRoot);
                root.add( node );
                //showChildren(node);
                //
                File[] files = fileSystemView.getFiles(fileSystemRoot, true);
                for (File file : files) {
                    if (file.isDirectory()) {
                        node.add(new DefaultMutableTreeNode(file));
                    }
                }
                //
            }

            tree = new JTree(treeModel);
            tree.setRootVisible(false);
            tree.addTreeSelectionListener(treeSelectionListener);
            tree.setCellRenderer(new FileTreeCellRenderer());
            tree.expandRow(0);
            JScrollPane treeScroll = new JScrollPane(tree);

            // as per trashgod tip
            tree.setVisibleRowCount(15);

            Dimension preferredSize = treeScroll.getPreferredSize();
            Dimension widePreferred = new Dimension(
                200,
                (int)preferredSize.getHeight());
            treeScroll.setPreferredSize( widePreferred );

            // Chi tiết Files/Folders
            JPanel fileMainDetails = new JPanel(new BorderLayout(4,2));
            fileMainDetails.setBorder(new EmptyBorder(0,6,0,6));

            JPanel fileDetailsLabels = new JPanel(new GridLayout(0,1,2,2));
            fileMainDetails.add(fileDetailsLabels, BorderLayout.WEST);

            JPanel fileDetailsValues = new JPanel(new GridLayout(0,1,2,2));
            fileMainDetails.add(fileDetailsValues, BorderLayout.CENTER);
            
            //Trường File name
            fileDetailsLabels.add(new JLabel("File", JLabel.TRAILING));
            fileName = new JLabel();
            fileDetailsValues.add(fileName);
            
            //Trường Path Name
            fileDetailsLabels.add(new JLabel("Path/name", JLabel.TRAILING));
            path = new JTextField(5);
            path.setEditable(false);
            fileDetailsValues.add(path);
            
            //Trường Last Modified
            fileDetailsLabels.add(new JLabel("Last Modified", JLabel.TRAILING));
            date = new JLabel();
            fileDetailsValues.add(date);
            
            //Trường File Size
            fileDetailsLabels.add(new JLabel("File size", JLabel.TRAILING));
            size = new JLabel();
            fileDetailsValues.add(size);
            
            //Hiển thị loại File hay Folder.
            fileDetailsLabels.add(new JLabel("Type", JLabel.TRAILING));

            JPanel flags = new JPanel(new FlowLayout(FlowLayout.LEADING,4,0));
            isDirectory = new JRadioButton("Directory");
            isDirectory.setEnabled(false);
            flags.add(isDirectory);

            isFile = new JRadioButton("File");
            isFile.setEnabled(false);
            flags.add(isFile);
            fileDetailsValues.add(flags);

            int count = fileDetailsLabels.getComponentCount();
            for (int ii=0; ii<count; ii++) {
                fileDetailsLabels.getComponent(ii).setEnabled(false);
            }

            //Các nút Open/Edit/New/Rename/Delete...
            JToolBar toolBar = new JToolBar();
            
            toolBar.setFloatable(false);

            //Khởi tạo và hiển thị nút Open
            openFile = new JButton("Open");
            openFile.setMnemonic('o');

            openFile.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent ae) {
                    try {
                        desktop.open(currentFile);
                    } catch(Throwable t) {
                        showThrowable(t);
                    }
                    gui.repaint();
                }
            });
            toolBar.add(openFile);
            
           

            // Kiểm tra các hành động và cho phép thực thi.
            openFile.setEnabled(desktop.isSupported(Desktop.Action.OPEN));
            
            
            //Đường phân dọc vs các nút trên.
            toolBar.addSeparator();
            
            //Khởi tạo và hiển thị nút New.
            newFile = new JButton("New");
            newFile.setMnemonic('n');
            newFile.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent ae) {
                    newFile();
                }
            });
            toolBar.add(newFile);

            //Khởi tạo và hiển thị nút Copy
            copyFile = new JButton("Copy");
            copyFile.setMnemonic('c');
            
            copyFile.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent ae) {
                    int row = table.getSelectionModel().getLeadSelectionIndex();
                    Source=((FileTableModel)table.getModel()).getFile(row);
                    //showMessage("Copied!", "Copy Notification");
                    Flag_Paste=true;
                    showMessage("Copy From :"+Source.getName(), " Copy Notification");
                }
                
                
            });
            toolBar.add(copyFile);
            
            //Khởi tạo và hiển thị nút Cut
            cutFile = new JButton("Cut");
            cutFile.setMnemonic('c');
            
            cutFile.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent ae) {                   
                    int row = table.getSelectionModel().getLeadSelectionIndex();
                    Source=((FileTableModel)table.getModel()).getFile(row);
                    //showMessage("Copied!", "Copy Notification");
                    Flag_Paste=false;
                    showMessage("Cut From: "+Source.getName(), " Cut Notification");
                }                                
            });
            toolBar.add(cutFile);
            
            
           
            
            //Khởi tạo và hiển thị nút Paste
            pasteFile = new JButton("Paste");
            pasteFile.setMnemonic('p');
            
            pasteFile.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent ae) {
                        TreePath parentPath = findTreePath(currentFile.getParentFile());
                        System.out.println("parentPath: " + parentPath);
                        DefaultMutableTreeNode parentNode =
                        (DefaultMutableTreeNode)parentPath.getLastPathComponent();
                        System.out.println("parentNode: " + parentNode);
                       
                        int row = table.getSelectionModel().getLeadSelectionIndex();
                        Dest=((FileTableModel)table.getModel()).getFile(row);
                        
                        
                        if(Flag_Paste)
                        {
                            showMessage("Copy To :"+Dest.getName(), " Copy Notification");
                            try 
                                {
                                    copy(Source, Dest);
                                } 
                            catch (IOException ex) 
                                {
                                    Logger.getLogger(FileManager.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            showMessage("Copy Complete!", " Copy Notification");
                        }
                        else
                        {
                            showMessage("Cut To: "+Dest.getName(), " Copy Notification");
                            try 
                                {
                                    cut(Source, Dest);
                                } 
                            catch (IOException ex) 
                                {
                                    Logger.getLogger(FileManager.class.getName()).log(Level.SEVERE, null, ex);
                                }
                             showMessage("Cut Complete!", "Cut Notification");
                        }
                        
                        showChildren(parentNode);
                }
                
                
            });
            toolBar.add(pasteFile);

            //Khởi tạo và hiển thị nút Rename.
            JButton renameFile = new JButton("Rename");
            renameFile.setMnemonic('r');
            renameFile.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent ae) {
                    renameFile();
                }
            });
            toolBar.add(renameFile);

            //Khởi tạo và hiển thị nút Delete.
            deleteFile = new JButton("Delete");
            deleteFile.setMnemonic('d');
            deleteFile.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent ae) {
                    deleteFile();
                }
            });
            toolBar.add(deleteFile);

            toolBar.addSeparator();

            //Các Checkbox Read/Write/Excute.
            readable = new JCheckBox("Read  ");
            readable.setMnemonic('a');
            //readable.setEnabled(false);
            toolBar.add(readable);

            writable = new JCheckBox("Write  ");
            writable.setMnemonic('w');
            //writable.setEnabled(false);
            toolBar.add(writable);

            executable = new JCheckBox("Execute");
            executable.setMnemonic('x');
            //executable.setEnabled(false);
            toolBar.add(executable);

            JPanel fileView = new JPanel(new BorderLayout(3,3));

            fileView.add(toolBar,BorderLayout.NORTH);
            fileView.add(fileMainDetails,BorderLayout.CENTER);

            detailView.add(fileView, BorderLayout.SOUTH);

            JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                treeScroll,
                detailView);
            gui.add(splitPane, BorderLayout.CENTER);

            JPanel simpleOutput = new JPanel(new BorderLayout(3,3));
            progressBar = new JProgressBar();
            simpleOutput.add(progressBar, BorderLayout.EAST);
            progressBar.setVisible(false);

            gui.add(simpleOutput, BorderLayout.SOUTH);

        }
        return gui;
    }
    
    
    /*CHẠY TRONG MAIN*/
    public void showRootFile() {
        // ensure the main files are displayed
        tree.setSelectionInterval(0,0);
    }

    
    /*BY :CAO VAN HOA*/
    
    private TreePath findTreePath(File find) {
        for (int ii=0; ii<tree.getRowCount(); ii++) {
            TreePath treePath = tree.getPathForRow(ii);
            Object object = treePath.getLastPathComponent();
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)object;
            File nodeFile = (File)node.getUserObject();

            if (nodeFile==find) {
                return treePath;
            }
        }
        // not found!
        return null;
    }

    //Hàm Rename File.-Chạy trong sự kiện click button Rename
    private void renameFile() {
        if (currentFile==null) {
            showErrorMessage("No file selected to rename.","Select File");
            return;
        }

        //Hiển thị Gui thông báo gồm 1 Input
        String renameTo = JOptionPane.showInputDialog(gui, "New Name");
        if (renameTo!=null) {
            try {
                boolean directory = currentFile.isDirectory();
                TreePath parentPath = findTreePath(currentFile.getParentFile());
                DefaultMutableTreeNode parentNode =
                    (DefaultMutableTreeNode)parentPath.getLastPathComponent();

                boolean renamed = currentFile.renameTo(new File(
                    currentFile.getParentFile(), renameTo));
                if (renamed) {
                    if (directory) {
                        // rename the node..

                        // delete the current node..
                        TreePath currentPath = findTreePath(currentFile);
                        System.out.println(currentPath);
                        DefaultMutableTreeNode currentNode =
                            (DefaultMutableTreeNode)currentPath.getLastPathComponent();

                        treeModel.removeNodeFromParent(currentNode);

                        // add a new node..
                    }

                    showChildren(parentNode);
                } else {
                    String msg = "The file '" +
                        currentFile +
                        "' could not be renamed.";
                    showErrorMessage(msg,"Rename Failed");
                }
            } catch(Throwable t) {
                //showThrowable(t);
            }
        }
        gui.repaint();
    }

    //Hàm Delete File.-Chạy trong sự kiện click button Delete.
    private void deleteFile() {
        if (currentFile==null) {
            showErrorMessage("No file selected for deletion.","Select File");
            return;
        }
        
        /*Tạo bảng Delete File*/
        int result = JOptionPane.showConfirmDialog(
            gui,
            "Are you sure you want to delete this file?",
            "Delete File",
            JOptionPane.ERROR_MESSAGE
            );
        if (result==JOptionPane.OK_OPTION) {
            try {
                System.out.println("currentFile: " + currentFile);
                TreePath parentPath = findTreePath(currentFile.getParentFile());
                System.out.println("parentPath: " + parentPath);
                DefaultMutableTreeNode parentNode =
                    (DefaultMutableTreeNode)parentPath.getLastPathComponent();
                System.out.println("parentNode: " + parentNode);

                boolean directory = currentFile.isDirectory();
                boolean deleted = currentFile.delete();
                if (deleted) {
                    if (directory) {
                        // delete the node..
                        TreePath currentPath = findTreePath(currentFile);
                        System.out.println(currentPath);
                        DefaultMutableTreeNode currentNode =
                            (DefaultMutableTreeNode)currentPath.getLastPathComponent();

                        treeModel.removeNodeFromParent(currentNode);
                    }
                    
                } 
                else 
                {
                    /*
                    String msg = "The file '" +
                        currentFile +
                        "' could not be deleted.";
                    showErrorMessage(msg,"Delete Failed");
                    */
                    Delete_Folder(currentFile);                                        
                }
             showChildren(parentNode);
            } catch(Throwable t) {
                //showThrowable(t);
            }
        }
        gui.repaint();
    }

    //Hàm New File.-Chạy trong sự kiện click button NewFile
    private void newFile() {
        if (currentFile==null) {
            showErrorMessage("No location selected for new file.","Select Location");
            return;
        }
        /*Tạo bảng nhập Tên File/Folder  mới*/
        if (newFilePanel==null) {
            newFilePanel = new JPanel(new BorderLayout(3,3));

            JPanel southRadio = new JPanel(new GridLayout(1,0,2,2));
            newTypeFile = new JRadioButton("File", true);
            JRadioButton newTypeDirectory = new JRadioButton("Directory");
            ButtonGroup bg = new ButtonGroup();
            bg.add(newTypeFile);
            bg.add(newTypeDirectory);
            southRadio.add( newTypeFile );
            southRadio.add( newTypeDirectory );

            name = new JTextField(15);

            newFilePanel.add( new JLabel("Name"), BorderLayout.WEST );
            newFilePanel.add( name );
            newFilePanel.add( southRadio, BorderLayout.SOUTH );
        }

        int result = JOptionPane.showConfirmDialog(
            gui,
            newFilePanel,
            "Create File",
            JOptionPane.OK_CANCEL_OPTION);
        if (result==JOptionPane.OK_OPTION) {
            try {
                boolean created;
                File parentFile = currentFile;
                if (!parentFile.isDirectory()) {
                    parentFile = parentFile.getParentFile();
                }
                File file = new File( parentFile, name.getText() );
                if (newTypeFile.isSelected()) {
                    created = file.createNewFile();
                } else {
                    created = file.mkdir();
                }
                if (created) {

                    TreePath parentPath = findTreePath(parentFile);
                    DefaultMutableTreeNode parentNode =
                        (DefaultMutableTreeNode)parentPath.getLastPathComponent();

                    if (file.isDirectory()) {
                        // add the new node..
                        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(file);

                        TreePath currentPath = findTreePath(currentFile);
                        DefaultMutableTreeNode currentNode =
                            (DefaultMutableTreeNode)currentPath.getLastPathComponent();

                        treeModel.insertNodeInto(newNode, parentNode, parentNode.getChildCount());
                    }

                    showChildren(parentNode);
                } else {
                    String msg = "The file '" +
                        file +
                        "' could not be created.";
                    showErrorMessage(msg, "Create Failed");
                }
            } catch(Throwable t) {
                showThrowable(t);
            }
        }
        gui.repaint();
    }
    
    //Hàm Copy-chạy trong sự kiện click button copy
    private void copy(File source, File dest) throws FileNotFoundException, IOException
    {
      if(source.isFile())
      {
          String name=source.getName();
          File Flag=new File(dest,name);
          copy_File(source, Flag);
      }
      else
      {
          String name=source.getName();
          File Flag=new File(dest,name);
          copy_Folder(source, Flag);
      }
      
    }
    
    //Hàm Copy File
    private static void copy_File(File source, File dest) throws IOException 
    {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            is.close();
            os.close();
        }
    }
    
    //Hàm Copy Folder
    private void copy_Folder(File src,File dest) throws IOException
    {
        if(src.isDirectory())
        {
            if(!dest.exists())
                dest.mkdir();
            
            String files[]=src.list();
        
            for(String file:files)
            {
                File srcFile=new File(src,file);
                File destFile= new File(dest,file);

                copy_Folder(srcFile,destFile);
            }
        }
        else
        {
            Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        
        
    }
    
    //Hàm Delete Folder
    private void Delete_Folder(File src)
    {
        File [] contents=src.listFiles();
        if(contents!=null)
        {
            for(File f:contents)
                Delete_Folder(f);
        }
        src.delete();
    }
    
    //Hàm Cut-chạy trong sự kiện click button Cut
    private void cut(File source, File dest) throws IOException
    {
        if(source.isFile())
      {
          String name=source.getName();
          File Flag=new File(dest,name);
          copy_File(source, Flag);
          source.delete();
      }
      else
      {
          String name=source.getName();
          File Flag=new File(dest,name);
          copy_Folder(source, Flag);
          Delete_Folder(source);
          
      }
    }
    
     /*END BY CAO VAN HOA*/

    //Thông báo Lỗi
    private void showErrorMessage(String errorMessage, String errorTitle) {
        JOptionPane.showMessageDialog(
            gui,
            errorMessage,
            errorTitle,
            JOptionPane.ERROR_MESSAGE
            );
    }
    
    //Thông báo Message
    private void showMessage(String Message,String Titles)
    {
        JOptionPane.showMessageDialog(
                gui,
                Message,
                Titles, 
                JOptionPane.INFORMATION_MESSAGE
        );
    }
    
    
    //Thông báo Lỗi-khi gặp trường Throws
    private void showThrowable(Throwable t) {
        t.printStackTrace();
        JOptionPane.showMessageDialog(
            gui,
            t.toString(),
            t.getMessage(),
            JOptionPane.ERROR_MESSAGE
            );
        gui.repaint();
    }

   
    
    // Cập nhật nội dung của table khi thay đổi 
    private void setTableData(final File[] files) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (fileTableModel==null) {
                    fileTableModel = new FileTableModel();
                    table.setModel(fileTableModel);
                }
                table.getSelectionModel().removeListSelectionListener(listSelectionListener);
                fileTableModel.setFiles(files);
                table.getSelectionModel().addListSelectionListener(listSelectionListener);
                if (!cellSizesSet) {
                    Icon icon = fileSystemView.getSystemIcon(files[0]);

                    // size adjustment to better account for icons
                    table.setRowHeight( icon.getIconHeight()+rowIconPadding );

                    setColumnWidth(0,-1);
                    setColumnWidth(3,60);
                    table.getColumnModel().getColumn(3).setMaxWidth(120);
                    setColumnWidth(4,-1);
                    setColumnWidth(5,-1);
                    setColumnWidth(6,-1);
                    setColumnWidth(7,-1);
                    setColumnWidth(8,-1);
                    setColumnWidth(9,-1);

                    cellSizesSet = true;
                }
            }
        });
    }

    
    private void setColumnWidth(int column, int width) {
        TableColumn tableColumn = table.getColumnModel().getColumn(column);
        if (width<0) {
            // use the preferred width of the header..
            JLabel label = new JLabel( (String)tableColumn.getHeaderValue() );
            Dimension preferred = label.getPreferredSize();
            // altered 10->14 as per camickr comment.
            width = (int)preferred.getWidth()+14;
        }
        tableColumn.setPreferredWidth(width);
        tableColumn.setMaxWidth(width);
        tableColumn.setMinWidth(width);
    }

    // Add the files that are contained within the directory of this node.
    //Cập nhật lại các thay đổi khi thực hiện xong thao tác-GỌI HÀM NÀY KHI THỰC HIỆN THAO TÁC
    private void showChildren(final DefaultMutableTreeNode node) {
        tree.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);

        SwingWorker<Void, File> worker = new SwingWorker<Void, File>() {
            @Override
            public Void doInBackground() {
                File file = (File) node.getUserObject();
                if (file.isDirectory()) {
                    File[] files = fileSystemView.getFiles(file, true); //!!
                    if (node.isLeaf()) {
                        for (File child : files) {
                            if (child.isDirectory()) {
                                publish(child);
                            }
                        }
                    }
                    setTableData(files);
                }
                return null;
            }

            @Override
            protected void process(List<File> chunks) {
                for (File child : chunks) {
                    node.add(new DefaultMutableTreeNode(child));
                }
            }

            @Override
            protected void done() {
                progressBar.setIndeterminate(false);
                progressBar.setVisible(false);
                tree.setEnabled(true);
            }
        };
        worker.execute();
    }

    /*CHẠY TRONG MAIN*/
    //Hàm hiển thị chi tiết các nội dung của File/Folder. 
    private void setFileDetails(File file) {
        currentFile = file;
        Icon icon = fileSystemView.getSystemIcon(file);
        fileName.setIcon(icon);
        fileName.setText(fileSystemView.getSystemDisplayName(file));
        path.setText(file.getPath());
        date.setText(new Date(file.lastModified()).toString());
        size.setText(file.length() + " bytes");
        readable.setSelected(file.canRead());
        writable.setSelected(file.canWrite());
        executable.setSelected(file.canExecute());
        isDirectory.setSelected(file.isDirectory());

        isFile.setSelected(file.isFile());

        JFrame f = (JFrame)gui.getTopLevelAncestor();
        if (f!=null) {
            f.setTitle(
                APP_TITLE +
                " :: " +
                fileSystemView.getSystemDisplayName(file) );
        }

        gui.repaint();
    }
    
    
    /*Hàm Main-Chạy chương trình*/
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    // Significantly improves the look of the output in
                    // terms of the file names returned by FileSystemView!
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch(Exception weTried) {
                }
                JFrame f = new JFrame(APP_TITLE);
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                FileManager fileManager = new FileManager();
                f.setContentPane(fileManager.getGui());

                try {
                    URL urlBig = fileManager.getClass().getResource("fm-icon-32x32.png");
                    URL urlSmall = fileManager.getClass().getResource("fm-icon-16x16.png");
                    ArrayList<Image> images = new ArrayList<Image>();
                    images.add( ImageIO.read(urlBig) );
                    images.add( ImageIO.read(urlSmall) );
                    f.setIconImages(images);
                } catch(Exception weTried) {}

                f.pack();
                f.setLocationByPlatform(true);
                f.setMinimumSize(f.getSize());
                f.setVisible(true);

                fileManager.showRootFile();
            }
        });
    }
}

// A TableModel hiển thị File/Folder -khi chọn Folder bên cây thư mục. 
class FileTableModel extends AbstractTableModel {

    private File[] files;
    private FileSystemView fileSystemView = FileSystemView.getFileSystemView();
    private String[] columns = {
        "Icon",
        "File",
        "Path/name",
        "Size",
        "Last Modified",
        "R",
        "W",
        "E",
        "D",
        "F",
    };

    FileTableModel() {
        this(new File[0]);
    }

    FileTableModel(File[] files) {
        this.files = files;
    }

    public Object getValueAt(int row, int column) {
        File file = files[row];
        switch (column) {
            case 0:
                return fileSystemView.getSystemIcon(file);
            case 1:
                return fileSystemView.getSystemDisplayName(file);
            case 2:
                return file.getPath();
            case 3:
                return file.length();
            case 4:
                return file.lastModified();
            case 5:
                return file.canRead();
            case 6:
                return file.canWrite();
            case 7:
                return file.canExecute();
            case 8:
                return file.isDirectory();
            case 9:
                return file.isFile();
            default:
                System.err.println("Logic Error");
        }
        return "";
    }

    public int getColumnCount() {
        return columns.length;
    }

    public Class<?> getColumnClass(int column) {
        switch (column) {
            case 0:
                return ImageIcon.class;
            case 3:
                return Long.class;
            case 4:
                return Date.class;
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
                return Boolean.class;
        }
        return String.class;
    }

    public String getColumnName(int column) {
        return columns[column];
    }

    public int getRowCount() {
        return files.length;
    }

    public File getFile(int row) {
        return files[row];
    }

    public void setFiles(File[] files) {
        this.files = files;
        fireTableDataChanged();
    }
}

/* Hiển thị cây Folder. */
class FileTreeCellRenderer extends DefaultTreeCellRenderer {

    private FileSystemView fileSystemView;

    private JLabel label;

    FileTreeCellRenderer() {
        label = new JLabel();
        label.setOpaque(true);
        fileSystemView = FileSystemView.getFileSystemView();
    }

    @Override
    public Component getTreeCellRendererComponent(
        JTree tree,
        Object value,
        boolean selected,
        boolean expanded,
        boolean leaf,
        int row,
        boolean hasFocus) {

        DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
        File file = (File)node.getUserObject();
        label.setIcon(fileSystemView.getSystemIcon(file));
        label.setText(fileSystemView.getSystemDisplayName(file));
        label.setToolTipText(file.getPath());

        if (selected) {
            label.setBackground(backgroundSelectionColor);
            label.setForeground(textSelectionColor);
        } else {
            label.setBackground(backgroundNonSelectionColor);
            label.setForeground(textNonSelectionColor);
        }

        return label;
    }
}