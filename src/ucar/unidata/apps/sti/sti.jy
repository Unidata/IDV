
import javax.swing;

from javax.swing import JTextField
from javax.swing import JPasswordField
from javax.swing import JLabel
from ucar.unidata.util import GuiUtils;

def loadStiData(idv):
	hostField = JTextField('',30);
	userField = JTextField('',30);
	passwordField = JPasswordField('',30);
	comps = ArrayList();
	comps.add(JLabel("Database Host:"));
	comps.add(GuiUtils.inset(hostField,4));
	comps.add(JLabel("User Name:"));
	comps.add(GuiUtils.inset(userField,5));
	comps.add(JLabel("Password:"));
	comps.add(GuiUtils.inset(passwordField,5));
	contents = GuiUtils.doLayout(comps, 2,GuiUtils.WT_N, GuiUtils.WT_N);	
	contents =GuiUtils.inset(contents,5);
	ok = GuiUtils.showOkCancelDialog(None,'foo',	contents, None);
	if(ok==0):
		return;
	url = 'jdbc:mysql://' + hostField.getText() +':3306/typhoon?zeroDateTimeBehavior=convertToNull&user=' + userField.getText() +'&password=' + passwordField.getText();
	print(url);
	idv.makeOneDataSource(url,'DB.STORMTRACK', None);
