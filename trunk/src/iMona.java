import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.StringItem;
import javax.microedition.midlet.MIDlet;
import javax.microedition.rms.RecordStore;

import com.j_phone.io.BrowserConnection;
import com.j_phone.system.DeviceControl;
import com.jblend.io.InflateInputStream;
import com.jblend.micro.lcdui.LocalizedTextBox;
import com.jblend.micro.lcdui.LocalizedTextField;

/** iMona@zuzu
 * @author zuzu
 * @version  1.0.3
 */

public final class iMona extends MIDlet {
	/**�@iMona�̉�ʁ@*/
	mainCanvas canv;
	/** �A�v���̋N�� */
	public final void startApp() {  //��������n�܂�
		if(canv == null){	//�N����
			canv = new mainCanvas(this);
			canv.disp = Display.getDisplay(this);
			canv.disp.setCurrent(canv);  //�\��������N���X���w�肷��
		} else {	//�ĊJ
/*
			if((stat & 0x0000400) == 0){	//�ݒ蒆�ł͂Ȃ��ꍇ
				canv.commandAction(command[2], null);	//������
			}
*/
			canv.stat |= 0x1000000;	//��ʍX�V
		}

	}
	/**�@�A�v���̈ꎞ��~ */
	public final void pauseApp() {
		if(canv != null)
			canv.stat |= 0x1000000;	//��ʍX�V
	}
	/** �A�v���̏I�� */
	public final void destroyApp(boolean unconditional) {
		canv.saveSetting();
		canv.thread = null;
	}
} //classiMona�̏I���

/**
iMona�̑S�Ă̏������i�[����Ă���N���X
@author ��� & zuzu
@see iMona
*/
final class mainCanvas extends Canvas implements Runnable,CommandListener {
	/**	�������͎��Ɏg�p����form */
	Form inputForm;
	/**	�o�[�W�����p�ϐ� */
	String version = "1.0.3Xmas";
	/**	�F�X�ȂƂ���Ŏg���e�L�X�g�t�B�[���h�B���{����͌�����LocalizedTextField���g�p���Ă��܂��B */
	LocalizedTextField /*bname,*/ btitle, bres, bboard, bthread, tfield, tfield2;
	/**	�F�X�ȂƂ���Ŏg���e�L�X�g�{�b�N�X�B���{����͌�����LocalizedTextBox���g�p���Ă��܂��B */
	LocalizedTextBox tbox;
	/** �F�X�ȂƂ���ŌJ��Ԃ��g��ChoiceGroup */
	ChoiceGroup choice;
	/** BBS�f�B���N�g���� */
	String bbsname;

	/** �N�b�V���������N�̃��X�g��\�����邽�߂�String�ϐ� */
	String cushionlinklist = "zuzu�I\nimona.zuzu-service.net/ime.php?\n\n�����֎I\nime.k2y.info/?";
	//String searchurl = "";
	/** �������ݎ��ɓ��͂������e��ۑ����邽�߂�String�ϐ� */
	String name = "", mail = "", bodytext = "", title = "";
	
	/** cookie�����[����String�ϐ� **/
	String cookie = "";

	/**�@���ݎg�p���̒��ԃT�[�o�[��ۑ����邽�߂�String�ϐ��@*/
	String server = "";
	/** �g���I�v�V���� */
	String extendedoption = "";
	/** �N�b�V���������N */
	String cushionlink = "";
	//String resdata;
	byte resdata[];
	/** ���������� */
	String searchtext = "";
	//
	/** for�Ȃǂ̌J��Ԃ������������邽�߂̈ꎞ�ϐ� */
	int i_length; //for�Ȃǂ̌J��Ԃ��p�̈ꎞ�ϐ�

	/**�@�o�O�񍐂ׂ̈ɒǉ������ϐ��B���݂͎g�p���Ă��Ȃ��B�@*/
	StringBuffer bagdata = new StringBuffer("");
	//�ߋ��̖��O���͗���
	//String Namelist[] = new String[10];
	//int Namenum = 0;
	/** NG���[�h����pString�ϐ�	 */
	String ngword;
	//String textdata[] = new String[20];
	//cushionlinklist
	//name
	//mail
	//bodytext
	//title
	//server
	//�p�~�F�����N�͕K���s�̐擪�ɂ���B
	//����X���b�h��,���̃X���b�h,2ch�O

	/** *�ڍוs��<br>
	 * XXXYYYYZZZ<br>
     * XXX:�����N�̕�����(byte��)(Linklist[x] / 10000000)<br>
	 * YYYY:�����N�̂���s((Linklist[x] / 1000) % 10000)<br>
     * ZZZ:�����N�̂��錅(byte��)(Linklist[x] % 1000)�@*/
	int Linklist[] = new int[40];

	/** �ڍוs��<br>�@ZZZ:�����N�̂��錅(������)�@*/
	int Linklist2[] = new int[40];
	/**  �ڍוs��<br>�����N��URL�����[����z�� */
	String Linkurllist[] = new String[40];

	/**�@�ڍוs��<br>�����N�����i�[����Ƃ���B���g��BookMarkData�Ɠ��������B<br>
	 * n:����<br>
	�@*�@Linkref[3n+0]:�ԍ�<br>
	�@*�@Linkref[3n+1]:�X���b�h�ԍ�<br>
	�@*�@Linkref[3n+2]:���X�ԍ�<br>
	�@*�@Linkref[0],[1],[2]�͓ǂݍ������Ƃ��Ă���ԍ��A�X���b�h�ԍ��A���X�ԍ���\��<br>
	 * �@*/
	int Linkref[] = new int[50];	//�����N�����i�[����Ƃ���B���g��BookMarkData�Ɠ��������B
	//n:����
	//Linkref[3n+0]:�ԍ�
	//Linkref[3n+1]:�X���b�h�ԍ�
	//Linkref[3n+2]:���X�ԍ�
	//Linkref[0],[1],[2]�͓ǂݍ������Ƃ��Ă���ԍ��A�X���b�h�ԍ��A���X�ԍ���\��
	/** �ڍוs��<br> �����N�����i�[����Ƃ���B���g��BookMarkData�Ɠ��������B*/
	int  Linkrefsc[] = new int[20];	//�����N�����i�[����Ƃ���B���g��BookMarkData�Ɠ��������B
	//List alist;
	/** �ʐM�p��output */
	ByteArrayOutputStream outarray;
	/** �_�E�����[�h�����f�[�^���i�[����ꏊ */
	byte dlarray[];	//�_�E�����[�h�����f�[�^���i�[����ꏊ
	/** �f�[�^���i�[����ꏊ */
	byte brdarray[];	//�f�[�^���i�[����ꏊ
	/**�@�e�N���X�@�@*/
	iMona parent;

	/**�@MIDP�ŕK�{��Display�N���X�B�����ɑS�Ă�`�悷��B�@*/
	Display disp;

	/** ���X�g�{�b�N�X�B������e�탁�j���[�\���p�@�@*/
	String ListBox[];
	//-/String DataFolder[];
	/** ��ʂ̕� */
	int width;
	/**�@��ʂ̍��� */
	int height;
//	int nThread[];		//�X���b�h�̔ԍ����i�[����z��
//	String ThreadName[];//�X���b�h�����i�[����z��
//	int nRes[];			//�X���b�h�̃��X�����i�[����z��
	/**�@�ڍוs���B�ǂ����DivStr�p�̈ꎞ�ϐ����ۂ��@
	 * @see MainCanvas#DivStr*/
	int iDivStr[];// = new String[100];
	/**�@���ݕ\�����̃��X�̒��g�B�������s���Ȃ����ߌ��ɂ����B�@�@*/
	String resstr;
	/** �ڍוs���B���X���i�[����z�񂩂Ǝv����B */
	String DivStr[];// = new String[100];
//	byte Res[][];//���X���i�[����z��
	//String ResElements[];//���X���i�[����z��
	/**�@�ޯ�ϰ��̃^�C�g��������@*/
	String BookMark[] = new String[200];
	/**�@�ޯ�ϰ��̃{�[�h�ԍ�+�X���b�h�ԍ�+ڽ(��)�ԍ�[����BookMark.length*3] AAAABBBCCCC�@*/
	int BookMarkData[];
	/**�@�g���~���O�ς݂̃u�b�N�}�[�N���@*/
	String tBookMark[] = new String[200];
	/**�@�u�b�N�}�[�N�̐ڑ�URL�@*/
	String BookMarkUrl[] = new String[200];

	//�L���b�V���֌W
	/** ���ݓǂ�ł���L���b�V���̃C���f�b�N�X�B�L���b�V���n�̔z��͂�����L�[�ɂ��ēǂݍ��� */
	int nCacheIndex;
	/** ����A�X���b�h�^�C�g�� */
	String CacheTitle[] = new String[40];
	/** �X���b�h�̃f�[�^(�̃L���b�V���̎���null) */
	byte CacheResData[][][] = new byte[40][][];
	/** �̃f�[�^(�X���b�h�^�C�g��)(�X���b�h�̃L���b�V���̎���null) */
	String CacheBrdData[][] = new String[40][];
	/** �̃f�[�^(���X��)(�X���b�h�̃L���b�V���̎���null) */
	int nCacheBrdData[][] = new int[40][];
	/** �X���b�h�ԍ�(�X���b�h�̃L���b�V���̎���nCacheTh[x][0]���g��) */
	int nCacheTh[][] = new int[40][];
	/** �ԍ� */
	int nCacheBrd[] = new int[40];
	/** start */
	int nCacheSt[] = new int[40];
	/** to */
	int nCacheTo[] = new int[40];
	/**�@all */
	int nCacheAll[] = new int[40];
	/**�@status�@*/
	int nCacheStat[] = new int[40];
	/**�@time to live�B�Â��L���b�V���قǒl���傫���Ȃ�B(-1:���̃L���b�V���͖���or��)�@*/
	int nCacheTTL[] = new int[40];
	/**�@�L���b�V���̑��݃`�F�b�N�p�@*/
	int nCacheInfo[];
	/**
	 * �z�F�ݒ�p�B�ł��e�ʂ�������̂ł܂������B
	 */
	int ColPreset[][] = {//color scheme
	//    �w�i�F       �����F       �����F       ���Ԃ̔Z��   ؽđI��F    ؽđI��F(F) ���O(ڽ)     ���ӐF       �ݸ�F(�I��)  �ݸ�F(���I��) �ݸ�F(�I��,�����) �ݸ�F(�����)
	//    0   1   2    3   4   5    6   7   8    9   10  11   12  13  14   15  16  17   18  19  20   21  22  23   24  25  26   27  28  29    30  31  32        33  34  35
		{ 239,239,239,   0,  0,  0, 192,192,192, 128,128,128, 192,192,255, 255,165,  0,   0,128,  0, 204, 51,  0, 192,192,255, 255,192,192,  128,128,255,      255,128,128},	//�ʏ�(2ch��)
		{ 255,255,255,   0,  0,  0, 192,192,192, 128,128,128, 128,128,255, 255,128,128,   0,128,  0, 204, 51,  0, 128,128,255, 255,128,128,   64, 64,255,      255, 64, 64},	//�n�C�R���g���X�g
		{   0,  0,  0, 225,225,225, 148,148,148, 128,128,128, 140,140,140, 128,128,128, 192,192,192, 204, 51,  0,  98, 98, 98,   0,  0,  0,  128,128,128,       64, 64, 64},	//���n�D��
		{ 255,251,244, 126, 94, 87, 175,142,149, 128,128,128, 249,163,150, 255,200,140, 195,146, 82, 195,146, 82, 250,173,160, 255,230,180,  255,148,148,      255,204,136},	//�g�F�n
	};
	/**
	 * ���݂̔z�F�ݒ�
	 */
	int[] ColScm;	//�z�F�ݒ�
	/**
	 * �T�[�o�[URL�p�̔z��
	 */
	String server_url[] = { //�IURL�z��
			"http://imona.coresv.com/",
			"http://imona.net/",
			"http://imona.zuzu-service.net/o/"
	};// = new String[100];
	/**
	 * data,SavNoInfo<br>
	 * o0 - �X���ꗗ�ň�x�ɓǂݍ��ސ�<br>
	 * o1 - ��x�ɓǂݍ��ރ��X�̐�<br>
	 * _2 - x���݌��Ă���X���̔ԍ�(��Βl)<br>
	 * _3 - x���݌��Ă���ԍ�(��Βl)<br>
	 * _4 - x���݌��Ă���X���̔ԍ�(���ݓǂݍ���ł��钆�̂͂��߂���̃C���f�b�N�X(0-data[0]))<br>
	 * _5 - x���݌��Ă���X���̔ԍ��̃C���f�b�N�X(0-400�`500)<br>
	 * _6 - ���݌��Ă��郌�X�ԍ�(data[7]����̑��Βl�A2ch�ł̃��X�ԍ�(��Βl)=data[6]+data[7])<br>
	 * _7 - x���݌��Ă���X���b�h�̃��X�̎n�܂�<br>
	 * _8 - x���݌��Ă���X���b�h�̃��X�̏I���<br>
	 * _9 - x���݌��Ă���X���b�h�̑S���X��<br>
	 * _10 -  ���X�g��������̈ړ���<br>
	 * _11 -  �I���̈ړ���<br>
	 * _12 -  LISTBOX Y���W<br>
	 * _13 -  LISTBOX �c��<br>
	 * _14 -  DL����T�C�Y<br>
	 * *15 -  �W�J��̃T�C�Y<br>
	 * _16 -  ����DL���Ă���W�J��̃T�C�Y(�i�s�`)<br>
	 * _17 -  gzip���k��̃T�C�Y<br>
	 * _18 -  ���k�O�̃T�C�Y<br>
	 * _19 -  �ݒ�(�ڍ�)�̑I���ʒu(�ړ���) ���X�g�{�b�N�X�̃��X�g�̐�<br>
	 * _20+21 -  �ݒ�(���C�����j���[)�̑I���ʒu<br>
	 * _22+23 -  �J�e�S�����X�g�̑I���ʒu<br>
	 * _24+25 -  ���X�g�̑I���ʒu<br>
	 * _26 -  �ݒ� / �I���o���鐔���̏��<br>
	 * _27 -  �ݒ� / �I�����Ă��錅<br>
	 * _28 -  �ݒ� / �I�����Ă���l<br>
	 * _29 -  �ݒ� / �I���o���鐔���̏���̌���-1<br>
	 * _30 -  �����̍���<br>
	 * _31 -  �����̕`��ʒu(�x�[�X���C���̈ʒu)font.getAscent<br>
	 * _32 -  �x�[�X���C�����牺 font.getDescent<br>
	 * _33 -  �����̕�(�����̍���/2[+1])<br>
	 * o34 -  �s��<br>
	 * o35 -  �P�X�N���[���ňړ������<br>
	 * o36 -  �����̃T�C�Y(0:�� 1:�� 2:��)<br>
	 * _37 -  �ǎ�x���W<br>
	 * _38 -  �ǎ�y���W<br>
	 * _39 -  �ǎ��̃T�C�Y<br>
	 * o40 -  �ޯ�ϰ��̐�<br>
	 * .41 -  �ǎ��̈ʒu<br>
	 * _42 -  SeparateString�ŋ�؂�Ƃ��̍ő剡���̃o�C�g��<br>
	 * _43 -  ���O�ɑI�������ޯ�ϰ��̃C���f�b�N�X<br>
	 * _44 -  ���X�g���ǂ��܂œǂݍ��񂾂�<br>
	 * _45 -  ���X���ǂ��܂ŏ���������<br>
	 * _46 -  �p�P�b�g��<br>
	 * o47 -  �p�P�b�g��(���v)<br>
	 * _48 -  ���X�N���[����(AA MODE�p)<br>
	 * o49 -  �ۑ�����ݒ�(byte 0xFF�܂�)<br>
	 * �@�@0x01 - �@���X�X�N���[�����Ƀ{�^�����������ςȂ��ŃX�N���[������<br>
	 * �@�@0x02 - �@�E��ɕb�̕\��<br>
	 * �@�@0x04 - �@ID�̕\��<br>
	 * �@�@0x08 - �@�����̕\��<br>
	 * �@�@0x10 - �@ұ�ނ̕\��<br>
	 * �@�@0x20 - �@SO�p��۰ُ���<br>
	 * �@�@0x40 - �@���O�̕\��<br>
	 * �@�@0x80 - �@keyrepeat xxx�ŐVڽ��1���ǂ�<br>
	 * _50 -  ������\����x���W<br>
	 * _51 -  ������\����y���W<br>
	 * _52 -  Box������\����x���W<br>
	 * _53 -  Box������\����y���W<br>
	 * _54 -  �y�[�W�X�N���[���̍s��<br>
	 * o55 -  �ŐVڽ�œǂސ�<br>
	 * o56 -  �ڏ��̕\��(2:���� 1:���Ȃ�) �ڈꗗ���ƭ���<br>
	 * o57 -  �ۑ�����t���O2<br>
	 * �@�@0x00000001 - �@���ON<br>
	 * �@�@0x00000002 - �@AA�̕\��<br>
	 * �@�@0x00000004 - �@URL�̕\��<br>
	 * �@�@0x00000008 - �@�E��Ɏ����̕\��(���F��)<br>
	 * �@�@0x00000010 - �@�w�ʉt���Ɏ��v��\��<br>
	 * �@�@0x00000020 - �@�w�ʉt�����[�h(ON:nomal OFF:mona)<br>
	 * �@�@0x00000040 - �@�w�ʉt����<br>
	 * �@�@0x00000100 - �@xxxʲ���׽�xxx�@�p�~<br>
	 * �@�@0x00000200 - �@������̎����X�V�@����������@�\<br>
	 * �@�@0x00000400 - �@�ǎ��̔�\��<br>
	 * �@�@0x00000800 - �@Ұْʒm�@�\<br>
	 * �@�@0x00001000 - �@����݂𖈉�ؒf<br>
	 * �@�@0x00002000 - �@�����폜�΍�<br>
	 * �@�@0x00004000 - �@��������̕\��<br>
	 * �@�@0x00008000 - �@1�ڂ��½�۰�(�X����)<br>
	 * �@�@0x00010000 - �@�X���ꗗ���A�I���L�[�ōŐV���X��ǂ�<br>
	 * �@�@0x00020000 - �@�߰�޽�۰�(�X����)<br>
	 * �@�@0x00040000 - �@�����\��<br>
	 *�@�@ 0x00080000 - �@���X�A���J�[��ON,OFF
	 * o58 -  �z�F�ݒ�<br>
	 * _59 -  Linklist�̎g�p��<br>
	 * _60 -  Linkfocus<br>
	 * _61 -  Link�o�C�g���J�E���g<br>
	 * _62 -  Link�̃X�^�[�g��<br>
	 * _63 -  Link�̃X�^�[�g�s<br>
	 * _64 -  Linkref�̎g�p��<br>
	 * _65 -  <br>
	 * o66 -  �ޯ�ϰ��̎g�p��(�Ԃ̋󔒂��܂�)<br>
	 * _67+68 -  �ޯ�ϰ��@�I���ʒu *�ǉ�*<br>
	 * o69 -  �p�P�b�g��̒P��(\/1000packet \0.3/packet->\300/1000packet) 0�̏ꍇ�͎���<br>
	 * _70 -  reserved<br>
	 * _71 -  reserved<br>
	 * _72 -  reserved<br>
	 * _73 -  reserved<br>
	 * _74 -  reserved<br>
	 * _75 -  reserved<br>
	 * o76 -  ���k��<br>
	 * _77 -  �X�N���[����<br>
	 * _78 -  �ǂݍ��ނ���(0:�T�[�o�[���I�� 1:���X 2:�X���b�h���X�g 3:��ʂ̃_�E�����[�h(text) 4:��ʂ̃_�E�����[�h(iMona�w�b�_����) 5:��������)<br>
	 * �@�@0x00000001 - �@���X<br>
	 * �@�@0x00000002 - �@�X���b�h���X�g<br>
	 * �@�@0x00000004 - �@�T�[�o�[���I��<br>
	 * �@�@0x00000008 - �@��ʂ̃_�E�����[�h(text)<br>
	 * �@�@0x00000010 - �@��������<br>
	 * �@�@0x00000100 - �@gzip���k�w��<br>
	 * �@�@0x00000200 - �@iMona�p�̃w�b�_����(iMona���k���Ȃ�)<br>
	 * _79 -  �擾���Ă����URL�̔ԍ�<br>
	 * o80 -  gzip�̈��k��<br>
	 * _81 -  httpinit���Ŏg�p����ꎞ�ϐ�<br>
	 * o82 -  �X���b�h�̃E�G�C�g<br>
	 * _83 -  �L�[���s�[�g�̃L�[<br>
	 * o84 -  ��ǂ݋@�\�Ő�ǂ݂��s�����X��<br>
	 * _85 -  DivStr�̎g�p��(DivStr�ŏ������Ă���s)<br>
	 * _86 -  ���݂̍s�̕�����(iDivStr�Ŏg�p����ϐ�)<br>
	 * o87 -  ���l�̐ݒ�<br>
	 * _88 -  �����N�ړ��̃E�G�C�g �L�[���s�[�g���̃E�G�C�g�{��<br>
	 * _89 -  <br>
	 * o90 -  ���܂łɓǂ񂾃��X�̐�<br>
	 * o91 -  �p�P�b�g��x��<br>
	 * o92 -  �X���b�h�ꗗ�ł̕\�����@<br>
	 * _93 -  �ʐM�\�ɂȂ�܂ł̑҂�����(�P�ʂ̓~���b,phase3.0)<br>
	 * o94 -  7�L�[�̋@�\<br>
	 * _95 -  �L�[����̂Ȃ�����<br>
	 * _96 -  ������ʂ�URL�̃u���E�U�W�����v<br>
	 * _97 -  �d��&�d�gϰ��̕\��<br>
	 * _98 -  �����X�^�C��<br>
	 * _99 -  �����t�H���g<br>
	 */
	int data[] = new int[100];

	//String strdata[3];
//	String CategoryList[];
//	String BoardList[];
//	String BoardList2[];
	/**
	* stat - �t���O���̈�<br>
	 * 0x0000001 -  ����������<br>
	 * 0x0000002 -  ���X�g�{�b�N�X�I���t���O<br>
	 * 0x0000004 -  �L�[�X�g�b�v<br>
	 * 0x0000008 -  �L�[����<br>
	 * 0x0000010 -  �ʐM��(�ʐM����)<br>
	 * 0x0000020 -  �ʐM�X�e�[�^�X�P�@�P�̂݁F�ڑ�����<br>
	 * 0x0000040 -  �ʐM�X�e�[�^�X�Q�@�Q�̂݁F�ڑ���������M��<br>
	 * 0x0000080 -  �ʐM�X�e�[�^�X�R�@�P�{�Q�{�R�F��M�����@�R�̂݁F�ʐM���s<br>
	 * 0x0000100 -  ���X�g�{�b�N�X�\��<br>
	 * 0x0000200 -  �f�[�^�t�H���_�̕\��<br>
	 * 0x0000400 -  �ݒ�_�C�A���O�̕\��(����or������or�F)<br>
	 * 0x0000800 -  ���ɃJ�e�S���ꗗ���擾���Ă���<br>
	 * 0x0001000 -  �ꗗ�擾��<br>
	 * 0x0002000 -  �J�e�S���I��<br>
	 * 0x0004000 -  �I��<br>
	 * 0x0008000 -  �X���ꗗ�̃����[�h			(**��ɂȂ���**�X���ꗗ�擾��(���߂Ď擾����ꍇ))<br>
	 * 0x0010000 -  �X���I��(+�X�������擾��)<br>
	 * 0x0020000 -  Loading���ɑ��삵�Ă���	(**��ɂȂ���**���X�擾��)<br>
	 * 0x0040000 -  ���X�\����(+���X�����擾��)<br>
	 * 0x0080000 -  �ʐM�X�g�b�v�v��<br>
	 * 0x0100000 -  ��X�N���[��ON<br>
	 * 0x0200000 -  ���X�N���[��ON<br>
	 * 0x0400000 -  �f�[�^�t�H���_�̓��e�\��<br>
	 * 0x0800000 -  �g�b�v�̍ĕ`��͍s��Ȃ�<br>
	 * 0x1000000 -  �S�̂̍ĕ`��<br>
	 * 0x2000000 -  �g�b�v�����ĕ`��<br>
	 * 0x4000000 -  ���X�g�����ĕ`��<br>
	 * 0x8000000 -  �ݒ莞�̍ĕ`��<br>
	 * 0x10000000 -  �ʐM��̏�����(�ʐM�͊���)<br>
	 * 0x20000000 -  ����M�̃��[������<br>
	 * 0x40000000 -  Loading���̑���͕s�\<br>
	 * 0x80000000 -  Loading���̑���͕s�\<br>
	 */
	int stat;
	/**
	* stat2 - �t���O���̓�<br>
	 * 0x0000001 -  �ݒ�<br>
	 * 0x0000002 -  �\���ݒ�<br>
	 * 0x0000004 -  ����ݒ�<br>
	 * 0x0000008 -  �ʐM�ݒ�<br>
	 * 0x0000010 -  �F�̐ݒ�<br>
	 * 0x0000020 -  ���̑�		/xxx/���j���[���X�g�ǂݍ��ݍς݃t���O/xxx/<br>
	 * 0x0000040 -  ���X�ԍ��w�胂�[�h<br>
	 * 0x0000080 -  AA MODE<br>
	 * 0x0000100 -  �������[�h<br>
	 * 0x0000200 -  �P�������[�h<br>
	 * 0x0000400 -  �F�ݒ胂�[�h<br>
	 * 0x0000800 -  ���X�ԍ��w�胂�[�h��-��\������ �t���O�������Ă��Ȃ�:-�L�� �t���O�������Ă���:-����<br>
	 * 0x0001000 -  �m�F�\��(�����L�[�������Ə�����)<br>
	 * 0x0002000 -  �����ݒ�ł̍ł��Ⴂ������1�ɂ���t���O<br>
	 * 0x0004000 -  Function(�A�X�^���X�N��������Ă���)<br>
	 * 0x0008000 -  �p�P�b�g��̏W�v<br>
	 * 0x0010000 -  �u�b�N�}�[�N�̕\��<br>
	 * 0x0020000 -  �u�b�N�}�[�N����X���b�h�փW�����v�������Ƃ������t���O<br>
	 * 0x0040000 -  �u�b�N�}�[�N�̕ҏW��<br>
	 * 0x0080000 -  ���ް�ݒ�ҏW���t���O<br>
	 * 0x0100000 -  SeparateString�ŕ�������Ƃ����t���O<br>
	 * 0x0200000 -  SeparateString�ŉ��s�R�[�h�ŉ��s�����Ƃ����t���O<br>
	 * 0x0400000 -  ���X�N���[��ON<br>
	 * 0x0800000 -  �E�X�N���[��ON<br>
	 * 0x1000000 -  �C���t�H���[�V�����\��<br>
	 * 0x2000000 -  DOJA�X�N���[���T�|�[�g<br>
	 * 0x4000000 -  �گ�ތ������<br>
	 * 0x8000000 -  �������ʕ\�����t���O<br>
	 * 0x10000000 -  �g����߼�ݕҏW���t���O<br>
	 * 0x20000000 -  �������݉�ʕ\�����t���O<br>
	 * 0x40000000 -  makeTL�ŏ��������s��<br>
	 * 0x80000000 -  makeRes�ŏ��������s��<br>
	 */
	int stat2;

	/**
	 * stat3 - �t���O���̎O<br>
	 * 0x0000001 -  LinkON(�����X���b�h��)<br>
	 * 0x0000002 -  LinkON(URL)<br>
	 * 0x0000004 -  LinkON(URL)�ŃW�����v�������Ƃ������t���O<br>
	 * 0x0000008 <br>
	 * 0x0000010 -  �c�k�������X�E�X���b�h���X�g���Ōォ�猩�邱�Ƃ������t���O<br>
	 * 0x0000020 -  �u�b�N�}�[�N����փW�����v�������Ƃ������t���O<br>
	 * 0x0000040 -  �u�߂�v�Ŗ߂��Ă��邱�Ƃ������t���O<br>
	 * 0x0000080 -  ��ǂ݂����邱�Ƃ������t���O �p�~�F�ꗗ������(�ēǍ����K�v)�ɂȂ������Ƃ������t���O<br>
	 * 0x0000100 -  �Q�ƌ���ۑ����Ȃ�<br>
	 * 0x0000200 -  �Q�ƌ���ۑ�����<br>
	 * 0x0000400 -  �u�b�N�}�[�N�̃��j���[�\��<br>
	 * 0x0000800 -  �ʐM�@�\�g�p��<br>
	 * 0x0001000 -  URL�w����<br>
	 * 0x0002000 -  �e�L�X�g�{�b�N�X<br>
	 * 0x0004000 -  ������ʂ�URL<br>
	 * 0x0008000 -  �L���b�V�������݂���<br>
	 * 0x0010000 -  �X�N���[������<br>
	 * 0x0100000 -  thread���L�p�F�ꗗ�擾<br>
	 * 0x0200000 -  thread���L�p�F�L�[���s�[�g<br>
	 * 0x0400000 -  <<�ڂ�<<�~�ɂ���<br>
	 * 0x0800000 -  ��>>��~>>�ɂ���<br>
	 * 0x1000000 -  �`�撆(���ׂ�����)<br>
	 * 0x2000000 -  �u�b�N�}�[�N�̃G�N�X�|�[�g ���X��`�悵�Ă���܂���x���L�[��������Ă��Ȃ�<br>
	 * 0x4000000 -  �u�b�N�}�[�N�̃C���|�[�g<br>
	 * 0x8000000 -  �u�b�N�}�[�N�̓��ʃ��j���[�\��<br>
	 */
	int stat3;

	/**
	 * stat4 - �t���O���̎l<br>
	0x0000001 - <br>
	0x0000002 - �ʐM�p�x�������z���Ă���(phase3.0)<br>
	0x0000004 - APDATA���o�p�t���O<br>
	0x0000008 - <br>
	0x0000010 - i�A�v���Ŕꗗ�̕ۑ���v������t���O(GraphicsMIDP2DOJA���ŗv������brdinit�ŕۑ�)<br>
	0x0000020 - <br>
	0x0000040 - <br>
	0x0000080 - <br>
	0x0000100 - 7�L�[�̋@�\(�ڗ�)�ݒ蒆<br>
	0x0000200 - 7�L�[�̋@�\(�ڗ�)���s��<br>
	0x0000800 - <br>
	0x0001000 - <br>
	0x0002000 - <br>
	0x0004000 - ������ݸ�ҏW��<br>
	0x0008000 - 0�L�[�̋@�\�ݒ蒆<br>
	0x0010000 - 0�L�[�̋@�\���s��<br>
	0x0020000 - NGword�ҏW��<br>
	0x0040000 - httpinit�ł̈���6�Ԗڗp�A�������݃t���O<br>
	 */
	int stat4;

	/**�@�ڍוs���B�@���Ԃ�data�ϐ��̃t���O���ĂԂ��߂̕⏕�ϐ����Ǝv����B*/
	int Powtable[] = { 1, 10, 100, 1000, 10000, 100000};

	/**
	 * �����I�ȕۑ����K�v��String�ϐ��p�̔z��
	 * 00 - �T�[�o��URL<br>
	 * 01 - ��URL(��̏ꍇ�͂܂��擾����Ă��Ȃ�)<br>
	 * 02 - setstr<br>
	 * 03 - �\�����Ă��郌�X�̖��O����<br>
	 * 04 - httpstr<br>
	 * 05 - sendstr<br>
	 * 06 - strtmp(main thread)<br>
	 * 07 - info������<br>
	 * 08 - �ǂݍ��ޔE�X���b�h�̃^�C�g��<br>
	 * 09 - ���ݕ\�����Ă���E�X���b�h�̃^�C�g��<br>
	 * 10 - �ǂݍ��ݒ��̔E�X���b�h�̃^�C�g��<br>
	 * 11 - �ʐM�G���[�̓��e<br>
	 */
	String strdata[] = new String[20];

	/** �ʐM�ȂǂɎg���X���b�h */
	Thread thread;

	/** iMona�ŕ`�悷��S�Ă̕�����ׂ̈̃t�H���g�B�������������Ε����`�����ς�� */
	Font font;

	/** �ǎ��p�C���[�W */
	Image wallpaper = null;

	//�o�b�N�o�b�t�@
	//Image backbuf;
	//Graphics bg;
	//�ƭ��\��
	//String MenuStr[] = { "�ꗗ", "�ޯ�ϰ�", "�ݒ�", "�I��"};
	//	String ThreadMenu[] = { "���̽ڂ�����", "�ޯ�ϰ��ɓo�^", "�ڈꗗ�̍ēǍ�", "�ꗗ�ɖ߂�"};
	//		String ResMenu[] = { "�ꗗ�ɖ߂�", "�ŐVڽ","ڽ�ԍ�", "�ޯ�ϰ�", "�ޯ�ϰ��ɓo�^", "�ݒ�", "�ƭ������"};
	//	String SettingMenu[] = { "X��ؽĂ̍X�V", "�ݒ�̕ۑ�", "�\���E����",/* "X�z�F",*/ "�ʐM�ݒ�", "!������!", "�߂�"};
	//		String ViewSetMenu[] = { "��������", "�s��", "��۰ٗ�", "��Ľ�۰�", "X�ǎ�", "�߂�"};
//			String ColSetMenu[] = { "�w�i�F", "�����F", "�I��F", "�g�F/�Z", "�g�F/��", "�߂�"};
			//String OpeSetMenu[] = { "�߰�޽�۰ٷ�" };
	//		String ComSetMenu[] = { "1��ɓǂ޽ڐ�", "1��ɓǂ�ڽ��", "X���k��", "�߂�"};
//	String StrList[9][] = { "����������", "��", "��", "��", "ttp://"};
	//String StrList[][] = new String[20][];
	/**
	 * ���j���[���ڗpString�z��
	 */
	String StrList[][] = {
	/*00*/	{},//temporary
	/*01*/	{"�ꗗ", "�ޯ�ϰ�", "URL�w��"/*, "�S�گ�ތ���*/, "�ݒ�", /*"�ް�̫���"*//*"BGM",*/ "�I��"},
	/*02*/	{/*"���̽ڂ��݂�", */"�ꗗ�ɖ߂�", "�ŐVڽ", "ڽ�Ԏw��", "1�̂ݎ擾", "�ޯ�ϰ��ɓo�^", "�گ�ނ𗧂Ă�", "�ēǍ�", "������ر", "�ʐM�̏ڍ�", /*"�ƭ������"*/"����"},
	/*03*/	{"�ꗗ�ɖ߂�", "�ŐVڽ", "ڽ�Ԏw��", "����",/*"�����Ƀ��X",*/ "������ʂ�URL", "�ޯ�ϰ�", "�ޯ�ϰ��ɓo�^", "�ݒ�", "÷���ޯ��", "÷���ޯ��(URL)", "������ر", "AAS", /* "�گ�ނ̏ڍ�",*/ "�ʐM�̏ڍ�", /*"�ƭ������"*/"����"},
	/*04*/	{"��ؽčX�V", "�ݒ�̕ۑ�", "�\���ݒ�", "����ݒ�", "�ʐM�ݒ�", "���̑�", "�߂�"},
	/*05*/	{"��������","��������","����̪��", "�s��", "�����\��", "�����\��", "�b�\��", /*"�ڈꗗ���ƭ���", */"�w�ʉt��", "ʲ���׽�", "�ǎ���\��", "��������̕\��", "�ڏ��̕\��", "�d�r�Ɠd�gϰ�", "�߂�"},
	/*06*/	{"��۰ٗ�", "��۰ٕ��@(�ڗ�)", "��Ľ�۰�", "SO�p��۰ُ���", "6,4����ڽ�ړ�(AAMode)", "�߂�"},
	/*07*/	{"1��ɓǂ޽ڐ�", "1��ɓǂ�ڽ��", "�ŐVڽ�œǂސ�"/*, "�ŐVڽ��1���ǂ�"*/, "gzip���k", "iMona���k", "ID�̕\��", "�����̕\��", "ұ�ނ̕\��", "���O�̕\��", "AA�̕\��", "URL�̕\��", "�߂�"},
	/*08*/	{"�߹�đ�̗݌v", "�݌v��ؾ��", "�߹�đ�̒P��", "�߹�đ�x��", "��ǂ݋@�\", "����������@�\", "Ұْ��M�̒ʒm", "�����폜�΍�", "�ŐVڽ�\��(�ڗ�)", "7���̋@�\(�ڗ�)", "7���̋@�\(�޸ϗ�)", "7���̋@�\(ڽ��)", "0���̋@�\", "���ް�ݒ�", "�g����߼��", "������ݸ", "������ʂ�URL", "��ɃA���J�[���X", /*"�ڷ���߰đ��x",*/"����", "NGܰ��(������)","�޸ޕ�",/* "!�޸Ϗ�����!",*/ "!������!", "��؏��", "�߂�"},

	/*09*/	{},
	/*10*/	{"��", "��", "��"},
	/*11*/	{"�~", "��"},
	/*12*/	{},
	/*13*/	{},
	/*14*/	{},
	/*15*/	{},
	/*16*/	{"���Ԃ��l�߂�", "���Ԃ����", "����߰�" , "���߰�", "�S����"},
	/*17*/	{"�ŐVڽ��ǂ�", "ڽ�Ԏw��", "����" , "������ʂ�URL", "�گ�ތ���", "�ҏW", "����"},
	/*18*/	{"�گ�ތ���", "�ҏW", "����"},

	/*19*/	{"�P�ʂ�", "\\/1000pckt�ł�" , "ex)\\0.3/p->300", "0:�����ݒ�"},
	/*20*/	{"�ݒ���z�𒴂�", "��Ƥ�x�����\��" , "����܂��"},
	/*21*/	{"�����Ŏw�肵��", "ڽ���O�ɂȂ��", "��ǂ݂����܂�", "0:��ǂ݂��Ȃ�"},
	/*22*/	{"ڽ��ǂݍ��ނ�", "�ޯ�ϰ�������", "�ōX�V���܂��"},
	/*23*/	{"Ұْ��M->���f", "iappli x504,5i", "ez+ phase2.5�p"},
	/*24*/	{"�����폜����", "�ŐVڽ��ǂ�", "���݂܂��"},
	/*25*/	{"0���w�肷���", "1��ɓǂ�ڽ��", "�Ɠ����l���g�p", "���܂��"},
	/*26*/	{"x505i,japp�p", "0:���k�Ȃ�", "�e��:1 > 9", "�����l:6"},
	/*27*/	{"1:��t�̂�", "2:����߰�", "3:���k�Ȃ�"},
	/*28*/	{"1:�S���\������", "2:AA�̂ݏ���", "3:AA�̂ݏ���2", "4:2+�S����", "5:3+�V"},
	/*29*/	{"1:�S���\������", "2:�ȗ�����", "3:2ch���̂ݕ\��", "4:���S����"},
	/*30*/	{"0:����", "1:Mona 2:Nomal", "3:1+�� 4:2+��"},
	/*31*/	{"�I�𷰂ōŐVڽ", "��ǂ݂܂��"},
	/*32*/	{"0:�ʏ�", "1:1�ڂ��½�۰�", "2:�߰�޽�۰�"},

	/*33*/  {"��:��׳�ނ��N��", "�~:���ڽ��\��"},
	/*34*/  {"�ؕb�Ŏw�肵��", "���������", "0:��\��"},
	/*35*/  {"��","��","��"},
	/*36*/  {"0:����","1:����","2:����߰����"},
	/*37*/  {"�������ޕύX(��)","�񸯼���ݸ�ެ���(ڽ)","����������ON,OFF(��)","�ޯ�ϰ��o�^(ڽ,��)","��ؽčX�V"},
	/*38*/  {"��:���ڽ�ݶ�", "�~:ڽ�ݶ���t���Ȃ�"}
		};
	//00 �e���|����
	//01 MenuStr
	//02 	ThreadMenu
	//03 		ResMenu
	//04 SettingMenu
	//05 	ViewSetMenu
	//06 	ComSetMenu
	//07 	OtherSetting
	//08  <reserved> color setting?
	//09 CompData
	//10 SetCharSize
	//11 SetChoose
	//12 CategoryList
	//13 BoardList
	//14 setcmstr
	//15 info
	//16
	//17
	//18
	//19


	/**
	 * �g�ъG�����p�z��
	 */
	String Emoji[] = {"\uE301 ", "\uE101 ", "\uE024 ", "ID:"};
//	byte Emoji[][] = {
//		{-29/*0xE3*/, 0x01, 0x20},
//		{-31/*0xE1*/, 0x01, 0x20},
//		{-32/*0xE0*/, 0x24, 0x20}
//		};

	//�_�u���o�b�t�@�����O
	//Graphics g;	//�o�b�N�o�b�t�@
	//Image    img2;
	Calendar calendar;

	HttpConnection co = null;
	
	/**
	 * iMona@zuzu��p�ϐ��ۑ��̈�
	 * 0.���X�ԍ��w��̕ۑ�
	 */
	int zuzu[] = new int[10];


	Command command[] = new Command[10];

	/**
	 * �N������̏����B�R�}���h�{�^���̐ݒ�A�ǎ��̕\���A��ʂ̉����c���̐ݒ�A�ݒ�ǂݍ��݁A�z�F�ݒ�Ȃǂ��s��
	 * @param p iMona
	 */
	public mainCanvas(iMona p){

		int key = 0;
		int i;
		parent = p;
		calendar = Calendar.getInstance();
		zuzu[0] = 0;


		command[0] = new Command("�ƭ�",1/*Command.SCREEN*/,0);
		command[1] = new Command("����",1/*Command.SCREEN*/,1);
		command[2] = new Command("���",1/*Command.SCREEN*/,2);
		command[3] = new Command("�ҏW",1/*Command.SCREEN*/,0);
		command[4] = new Command("�X�V",1/*Command.SCREEN*/,0);
		command[5] = new Command("Ұ�",1/*Command.SCREEN*/,5);
		command[6] = new Command("�߂�",1/*Command.SCREEN*/,5);
		command[7] = new Command("����",1/*Command.SCREEN*/,5);
		command[8] = new Command("���s",1/*Command.SCREEN*/,0);
		command[9] = new Command("�o�^",1/*Command.SCREEN*/,4);
		//command[9] = new Command("�ږ�",Command.SCREEN,0);
		//command[10] = new Command("��",Command.SCREEN,0);
		//command[11] = new Command("BM��",Command.SCREEN,0);
		setCommandListener(this);
		ListBox = StrList[1];
		//data[19] = ListBox.length;
		stat |= 0x0100;
		width = getWidth();
		height = getHeight();
		//�_�u���o�b�t�@�����O
		//backbuf = Image.createImage( width, height);
		//bg = backbuf.getGraphics();

		data[50] = width / 2;
		data[51] = height / 2;
		data[52] = width / 2;
		data[53] = height / 2;
		try {
			//�ǎ�
			data[37] = 0;	data[38] = 0;
			if(width < 240){
				wallpaper = Image.createImage("/m.png");
			} else {
				wallpaper = Image.createImage("/m2.png");
				data[37] = +10;
				data[38] = +5;
			}



			//�ǎ�
			data[37] += width / 2 - wallpaper.getWidth() / 2;	//default x
			//data[38] = 50;	//default y
			data[38] += height - 10 - wallpaper.getHeight();	//default y
		} catch(Exception e){
		}
		Load();	//���R�[�h�X�g�A����ǂݍ���
		//�z�F
		ColScm = ColPreset[data[58]];
		//if((data[57] & 0x00000100) != 0){
		//	data[57] ^= 0x00000100;
		//	data[58] = 1;
		//	ColScm = ColPreset[1];
		//}
		// else {
		//	data[58] = 0;
		//	ColScm = ColPreset[0];
		//}
		//System.out.println("gc " + (Runtime.getRuntime()).freeMemory());
		System.gc();	//�K�x�[�W�R���N�V����
		stat |= 0x1000000;	//�ĕ`��
		// �X���b�h�̐ݒ�
		Thread thread = new Thread(this);
		thread.start();
//#ifdef 	//
//#else
//		StrList[12] = CategoryList;	//�J�e�S��
//		StrList[13] = BoardList;	//��
//		stat |= 0x0000800;	//�J�e�S�����X�g�ǂݍ��ݍς݃t���O
//		data[44] = CATNUM;		//�����ׂēǂݍ��ݏI����Ă�
	//	if(StrList[12].length == 0){	//�܂��ꗗ���_�E�����[�h����Ă��Ȃ��ꍇ
	//		try {
	//			httpinit(0,0,0,0,0);//dlbrdlist();
	//		} catch(Exception e){
	//			strdata[7] = e.toString();
	//			stat2 |= 0x0001000;
	//		}
	//	}
//#endif
	}















//#ifdef 	//
/**
* �ꗗ�̓ǂݍ���
*/
	public final void brdinit(){
		int i;


		try{
//#ifdef 	//
			if(brdarray != null){
				//�ꗗ������������ǂݍ���
				String[] tmp = split(new String(brdarray), '\n');
				i_length = tmp.length;
				for(i = 0; i < i_length;i++){
					if(tmp[i].length() == 0){break;}
				}
				//�J�e�S���̐������������擾����
				StrList[12] = new String[i];//�J�e�S��
				StrList[13] = new String[i];//��
				System.arraycopy(tmp, 0, StrList[12], 0, i);
				stat |= 0x0000800;	//�J�e�S�����X�g�ǂݍ��ݍς݃t���O
				System.arraycopy(tmp, i+1, StrList[13], 0, tmp.length - i - 1);
				tmp = null;
			} else {
/*
				//�J�e�S���̐������������擾����
				StrList[12] = new String[CATNUM];//�J�e�S��
				StrList[13] = new String[CATNUM];//��
*/
				String[] tmp = new String[100];

//#ifdef DOJA	//DOJA
//				ByteArrayInputStream in = new ByteArrayInputStream(dlarray);
//#else
				InputStream in;
//				if(brdarray != null){
//					in = new ByteArrayInputStream(brdarray);
//				} else {
					//�ꗗ�����\�[�X����ǂݍ���
					in = getClass().getResourceAsStream("/cat.txt");
//				}
//#endif
				ByteArrayOutputStream out = new ByteArrayOutputStream(300);
				byte b[] = new byte[1];
				i = 0;
				while(true){
					int len;
					len = in.read(b);
					if(len < 0 || b[0] == '\n'){
						tmp[i] = out.toString();
						i++;
						out.reset();
						if(len < 0 || i == tmp.length/*�듮��h�~*/){
							break;
						}
					} else {
						out.write(b);
					}
				}
//#ifdef DOJA	//DOJA
//				in.read(b);
//#else
//				if(brdarray != null){
//					in.read(b);
//				} else {
					in.close();
//				}
//#endif
				StrList[12] = new String[i];//�J�e�S��
				System.arraycopy(tmp, 0, StrList[12], 0, i);
				stat |= 0x0000800;	//�J�e�S�����X�g�ǂݍ��ݍς݃t���O
				out.reset();
//#ifdef DOJA	//DOJA
//				in = this.getClass().getResourceAsStream("resource:///brd.txt");
//#else
				if(brdarray == null){
					in = getClass().getResourceAsStream("/brd.txt");
				}
				//data[44] = 0;
				i = 0;
				while(true){
					int len;
					len = in.read(b);
					if(len < 0 || b[0] == '\n'){
						//StrList[13][data[44]] = out.toString();
						//data[44]++;
						tmp[i] = out.toString();
						i++;
						out.reset();
						if(len < 0 || i/*data[44]*/ == tmp.length/*StrList[13].length*//*�듮��h�~*/){
							break;
						}
					} else {
						out.write(b);
					}
				}
				in.close();

				StrList[13] = new String[i];//��
				System.arraycopy(tmp, 0, StrList[13], 0, i);
			}

		} catch(Exception e){
		}
		System.gc();	//�K�x�[�W�R���N�V����

	}
//#endif
	//�R�}���h�C�x���g
	/**
	 * �R�}���h�C�x���g
	 * @param c Command
	 * @param s Displayable
	 */
	public final synchronized void commandAction(Command c,Displayable s) {
		commandAction(c);
	}

	/**
	 * �R�}���h�C�x���g
	 * @param c Command
	 */
	public final synchronized void commandAction(Command c) {
		int i;
		if((stat & 0x0000004) != 0){	//�L�[�X�g�b�v���������Ă�����
			return;
		}
		data[95] = 0;
		if((stat3 & 0x0000400) != 0){//�ޯ�ϰ����j���[
			i = data[67] + data[68];
		} else {
			i = data[10] + data[11];
		}
		if(c == command[0]){	//���j���[ KEY_SOFT1
			//if((stat2 & 0x0010000) != 0 && (stat2 & 0x0040000) == 0){//�ޯ�ϰ�
			//	ShowBookMarkMenu();
			//} else
			//System.out.println("stat2:" + stat2 + " 1:" + stat);
			if((stat2 & 0x0010000) != 0 && (stat2 & 0x0040000) == 0){//�ޯ�ϰ�
				if((stat3 & 0x0000400) == 0){//�ޯ�ϰ����j���[
					//ShowBookMarkMenu();
					showBookMark(1);
				}
			} else if((stat2 & 0x000070F) == 0 && (stat & 0x450000) != 0){
				if( (stat & 0x0100) != 0 ){	//���X�g�{�b�N�X
					stat ^= 0x0100;
				} else {
					stat |= 0x0100;
					data[10] = data[11] = 0;

					//if((stat2 & 0x0020000) != 0){
					//	ListBox[0] = "�ޯ�ϰ��ɖ߂�";
					//}
					removeCommand(command[6]);
					removeCommand(command[0]);
				}
				stat |= 0x1000000;	//��ʍX�V
			}
		} else if(c == command[6] && (stat & 0x10000010) == 0/* || c == command[9] || c == command[10] || c == command[11]*/){	//�߂�

			if(data[64] > 0){	//�Q�ƌ�������ꍇ
				stat3 |= 0x0000140;
				//stat3 |= 0x0000040;	//�߂�ŃA�N�Z�X���Ă���
				//stat3 |= 0x0000100;	//�Q�ƌ���ۑ����Ȃ�
				strdata[8] = "?";
				if(Linkref[data[64]*3+1] > 0){
					i = Linkref[data[64]*3+2] + data[1] - 1;
				} else {
					i = Linkref[data[64]*3+2] + data[0] - 1;
				}
				httpinit( 2, Linkref[data[64]*3], Linkref[data[64]*3+2],i,Linkref[data[64]*3+1]);
				thttpget();	//httpgetdata()��V�K�X���b�h�œ���
			} else if((stat2 & 0x0020000) != 0){	//�u�b�N�}�[�N����A�N�Z�X���Ă��ꍇ(���ڃX���b�h�ւ̃u�b�N�}�[�N)
				stat2 &= ~0x0020000;
				//data[77] = 0;
				stat &= ~0x00040000;	//�X�������Ă�t���O������
				showBookMark(0);
			} else if((stat & 0x10000) != 0){	//�X���b�h���X�g��\�����Ă���ꍇ
				backfromfirst();
				//stat |= 0x1000000;	//��ʍX�V
			} else if( (stat & 0x40000) != 0 ){	//ڽ���Ă鎞
				data[77] = 0;
				stat &= ~0x00040000;	//�X�������Ă�t���O������
				removeCommand(command[0]);
				removeCommand(command[6]);
				data[10] = 0;	data[11] = 0;
				ListBox = StrList[1];
				stat |= 0x0000100;
				stat |= 0x1000000;	//��ʍX�V
			}
		} else if(c == command[4]){	//�X�V
			if((stat2 & 0x0080000) != 0){	//���ް�ݒ�ҏW���t���O
				i = choice.getSelectedIndex();
				if(i == 0){
					server = server_url[0];
				} else if(i == 1){
					server = server_url[1];
				} else if(i == 2){
					server = server_url[2];
				} else {
					server = tfield.getString();
				}
				//SaveSetting();
				stat2 &= ~0x0080000;
			} else if((stat2 & 0x10000000) != 0){	//�g����߼�ݕҏW���t���O
				extendedoption = tfield.getString();
				//SaveSetting();
				stat2 &= ~0x10000000;
			}  else if((stat4 & 0x00004000) != 0){	//������ݸ�ҏW���t���O
				cushionlink = tfield.getString();
				//SaveSetting();
				stat4 &= ~0x00004000;
			} else if((stat4 & 0x00020000) != 0){
				ngword = tfield.getString();
				//SaveSetting();
				stat4 &= ~0x00020000;
			}
			else {
				EditBookMark( i + 1, btitle.getString(), Integer.parseInt(bboard.getString()), Integer.parseInt(bthread.getString()), Integer.parseInt(bres.getString()));
				//	strdata[7] = "�X�V���s";
				//	stat2 |= 0x0001000;
				inputForm = null;
				stat2 &= ~0x0040000;
			}
			disp.setCurrent(this);
			stat |= 0x1000000;	//��ʍX�V
		} else if(c == command[8]){	//���s
			if((stat2 & 0x4000000) != 0){//�گ�ތ������
				inputForm = null;
				disp.setCurrent(this);
				//stat |= 0x8000;	//�X���ꗗ�c�k��
				stat2 |= 0x8000000;	//�������ʕ\�����t���O
				strdata[8] = ListBox[data[10] + data[11]];
				strdata[6] = tfield.getString();
				searchtext = strdata[6];
				//if((stat3 & 0x0000400) == 0){//�ޯ�ϰ����j���[
				//	httpinit(4,(data[22] + data[23]) * 100 + data[10] + data[11],0,0,0);
				//} else {
				//	httpinit(4,BookMarkData[i * 3],0,0,0);
				//}

				httpinit(4,Linkref[0],1,0,0);
				thttpget();	//httpgetdata()��V�K�X���b�h�œ���
				stat2 &= ~0x4000000;
			} else if((stat3 & 0x0001000) != 0){//URL�w����
				disp.setCurrent(this);
				//strdata[6] = tfield.getString();
				stat3 |= 0x0000100;
				//httpinit(5, 0, 1, 0, 0);
				//thttpget();	//httpgetdata()��V�K�X���b�h�œ���
				Link(tfield.getString(), 0);
				thttpget();	//httpgetdata()��V�K�X���b�h�œ���
				stat3 &= ~0x0001000;
			} else if((stat2 & 0x20000000) != 0){	//�������
				disp.setCurrent(this);
				//�ҏW���e�̾���
				name = btitle.getString();
				mail = bres.getString();
				bodytext = bboard.getString();
				if((stat2 & 0x0010000) != 0){//�ޯ�ϰ�
					httpinit(7, BookMarkData[i * 3], 0, 0, BookMarkData[i * 3 + 1]);
				} else {
					httpinit(7, nCacheBrd[nCacheIndex]/*data[3]*/, 0, 0, nCacheTh[nCacheIndex][0]/*data[2]*/);
				}
				stat2 &= ~0x20000000;
			} else if((stat3 & 0x4000000) != 0){	//���߰�
				/*String test = tobox.getString();
				if(test.indexOf("iB2") == 0){

				}*/
				String[] buf = split(tbox.getString(), ',');
				if(buf[0].equals("iB2")){
					for(i = 1; i < buf.length;i+=5){
						if(EditBookMark(0, buf[i], Integer.parseInt(buf[i+1],36), Integer.parseInt(buf[i+2],36), Integer.parseInt(buf[i+3],36)) < 0){
							break;
						}
					}
				}else if(buf[0].equals("iB")){
					for(i = 1; i < buf.length;i+=4){
						if(EditBookMark(0, buf[i], Integer.parseInt(buf[i+1],36), Integer.parseInt(buf[i+2],36), Integer.parseInt(buf[i+3],36)) < 0){
							break;
						}
					}
				}
				disp.setCurrent(this);
				stat3 &= ~0x4000000;
			}/* else if((stat4 & 0x0008000) != 0){ //�S����
				i = choice.getSelectedIndex();
				if(i == 0){
					server = server_url[0];
				} else if(i == 1){
					server = server_url[1];
				}
				//SaveSetting();
				stat4 &= ~0x0008000;
			}*/
		} else if(c == command[7]){	//����

			tfield = new LocalizedTextField("��ܰ��",searchtext,20,LocalizedTextField.ANY);
			inputForm = new Form("= �گ�ތ��� =");
			if((stat3 & 0x0000400) != 0){//�ޯ�ϰ����j���[
				inputForm.append(new StringItem("�ԍ�:" + BookMarkData[i * 3],""));
				Linkref[0] = BookMarkData[i * 3];
			} else {
				inputForm.append(new StringItem("��:" + ListBox[i],""));
				Linkref[0] = (data[22] + data[23]) * 100 + i;
			}
			inputForm.append(tfield);

			inputForm.addCommand(command[8]);
			inputForm.addCommand(command[2]);

			inputForm.setCommandListener(this);

			disp.setCurrent(inputForm);

			stat2 |= 0x4000000;

		} else if(c == command[3]){	//�ҏW
			if((stat2 & 0x0010000) != 0 && (stat2 & 0x0040000) == 0){//�ޯ�ϰ�

				btitle = new LocalizedTextField("����",BookMark[i],100,LocalizedTextField.ANY);
				bres = new LocalizedTextField("ڽ�ԍ�(1-999)",""+BookMarkData[i * 3 + 2],4,LocalizedTextField.NUMERIC);
				bboard = new LocalizedTextField("�ԍ�(�ύX�s�v)",""+BookMarkData[i * 3],8,LocalizedTextField.NUMERIC);
				bthread = new LocalizedTextField("�ڔԍ�(�ύX�s�v)",""+BookMarkData[i * 3 + 1],16,LocalizedTextField.NUMERIC);
				inputForm = new Form("= �ҏW =");
				inputForm.append(btitle);
				inputForm.append(bres);
				inputForm.append(bboard);
				inputForm.append(bthread);

				inputForm.addCommand(command[4]);
				inputForm.addCommand(command[2]);

				inputForm.setCommandListener(this);

				disp.setCurrent(inputForm);

				stat2 |= 0x0040000;
			}
		} else if(c == command[9]){//���ޯ�ϰ��o�^
			if( (stat & 0x0004000) != 0 ){	//���X�g�ɂ���Ƃ�
				//strdata[9] = ListBox[data[10]+data[11]];
				//EditBookMark( 0,"[��]" + strdata[9], ((data[22] + data[23]) * 100 + data[10] + data[11]), 0, 0);
				EditBookMark( 0,"[��]" + ListBox[data[10]+data[11]], ((data[22] + data[23]) * 100 + data[10] + data[11]), 0, 0);
			//} else if((stat & 0x0010000) != 0){	//�X���I��
			//	EditBookMark( 0, CacheBrdData[nCacheIndex]/*ThreadName*/[data[4]], nCacheBrd[nCacheIndex]/*data[3]*/, nCacheTh[nCacheIndex]/*nThread*/[data[4]], 0);
			}
			//stat |= 0x1000000;

/*		} else if(c == command[1]){
			if((stat2 & 0x0010000) != 0){//�ޯ�ϰ��̏���
				i = data[10] + data[11];
				//if(BookMarkData[i * 3 + 1] != 0){	//���g�p�łȂ���
				EditBookMark( - i - 1, "", 0, 0, 0);	//����
				//}
				//removeCommand(command[1]);
				//removeCommand(command[3]);
			}
			//stat |= 0x1000000;	//��ʍX�V
*/
		} else if(c == command[2]){	//KEY_SOFT2	���
			///System.out.println("ST:CANCEL");
			if((stat & 0x0000400) != 0){	//�ݒ������
				stat &= ~0x0000400;//if((stat & 0x0000400) != 0){stat ^= 0x0000400;}

				stat2 &= ~0x0000740;//����4������
				//stat2 &= ~0x0000040;
				//stat2 &= ~0x0000100;//if((stat2 & 0x0000100) != 0){stat2 ^= 0x0000100;}
				//stat2 &= ~0x0000200;//if((stat2 & 0x0000200) != 0){stat2 ^= 0x0000200;}
				//stat2 &= ~0x0000400;//if((stat2 & 0x0000400) != 0){stat2 ^= 0x0000400;}
/*
				data[50] = width / 2;
				data[51] = height / 2;
				data[52] = width / 2;
				data[53] = height / 2;
*/
				stat2 &= ~0x1000000;

				removeCommand(command[2]);
//				if( (stat & 0x10000) != 0 ){	//�ڑI�𒆂̎�
//					addCommand(command[9]);
//					addCommand(command[6]);
//				}
			}
			else if((stat2 & 0x0010000) != 0 && (stat2 & 0x0040000) != 0){	//�ޯ�ϰ��X�V������
				stat2 &= ~0x0040000;
//					form = null;
				disp.setCurrent(this);
			} else if((stat2 & 0x4000000) != 0){	//����������
				stat2 &= ~0x4000000;
				searchtext = "";
				disp.setCurrent(this);
			} else if((stat2 & 0x0080000) != 0){	//���ް�ݒ�ҏW���t���O
				stat2 &= ~0x0080000;
				disp.setCurrent(this);
			} else if((stat2 & 0x10000000) != 0){	//�g����߼�ݕҏW���t���O
				stat2 &= ~0x10000000;
				disp.setCurrent(this);
			} else if((stat4 & 0x00004000) != 0){	//������ݸ�ҏW���t���O
				stat4 &= ~0x00004000;
				disp.setCurrent(this);
			} else if((stat4 & 0x00020000) != 0){
				stat4 &= ~0x00020000;
				disp.setCurrent(this);
			} else if((stat3 & 0x0001000) != 0){	//URL�w��
				stat3 &= ~0x0001000;
				disp.setCurrent(this);
			} else if((stat3 & 0x0002000) != 0){	//÷���ޯ��
				stat3 &= ~0x0002000;
				disp.setCurrent(this);
			} else if((stat3 & 0x0004000) != 0){	//������ʂ�URL
				stat3 &= ~0x0004000;
				disp.setCurrent(this);
			} else if((stat2 & 0x20000000) != 0){	//�������
				//�ҏW���e�̾���
				name = btitle.getString();
				mail = bres.getString();
				bodytext = bboard.getString();
				stat2 &= ~0x20000000;
				disp.setCurrent(this);
			} else if((stat3 & 0x2000000) != 0){	//����߰�
				stat3 &= ~0x2000000;
				disp.setCurrent(this);
			} else if((stat3 & 0x4000000) != 0){	//���߰�
				stat3 &= ~0x4000000;
				disp.setCurrent(this);
			}
			//setSoftLabel(Frame.SOFT_KEY_2, null);
			stat |= 0x1000000;	//��ʍX�V
		}
		//repaint();
	}
	/**
	 * �L�[���s�[�g
	 */
	public final void keyRepeated(int keyCode) {
		if( keyCode != 0 ){
			int action = getGameAction(keyCode);

			if((action == DOWN || action == UP || keyCode == KEY_NUM8 || keyCode == KEY_NUM2) && (stat & 0x0100) != 0){
				keyPressed(keyCode);
			}

			if((action == LEFT || action == RIGHT || keyCode == KEY_NUM4 || keyCode == KEY_NUM6) && (stat & 0x0040000) != 0){
				//System.out.println("2action:" + action);
				//wait(data[82]);
				//data[70] = 1;
				keyPressed(keyCode);
				//data[70] = 0;
			}
		}
	}
	//�L�[�v���X�C�x���g
	/**
	 * �L�[�v���X�C�x���g
	 */
	protected synchronized final void keyPressed(int keyCode) {
		int i, j = 0, k;

		data[95] = 0;


		int action = getGameAction(keyCode);
		if((stat & 0x0000004) != 0){	//�L�[�X�g�b�v���������Ă�����
			if(action == FIRE || keyCode == KEY_NUM5){	//�ʐM�̃L�����Z��
				stat |= 0x0080000;	//�ʐM���f
				try {
					if( (stat & 0x0020) != 0 && (stat & 0x0040) == 0 && co != null){co.close();	co = null;}
				} catch(Exception e) {}
				return;
			} else {
				if((stat2 & 0x0010000) != 0 || (stat & 0x40000000) != 0){//�u�b�N�}�[�NorLoading���̑���͕s�\
					return;
				} else {
					if(keyCode != KEY_NUM0){
						if((stat3 & 0x0000200) != 0){//�Q�ƌ���ۑ����Ă����ꍇ�͔j������
							stat3 &= ~0x0000200;
							if(data[64] > 0){data[64]--;}
						}
						stat |= 0x0020000;
					}
				}
			}
		}
/*
		if((stat2 & 0x0001000) != 0){//�m�F�\��(�����L�[�������Ə�����)
			return;
		}
*/
		if((stat2 & 0x0001000) != 0){//�m�F�\��(�����L�[�������Ə�����)
			stat2 ^= 0x0001000;
			strdata[7] = null;
			stat |= 0x1000000;	//��ʍX�V
			return;
		}
		//stat3 &= ~0x2000000;	//���X��`�悵�Ă���܂���x���L�[��������Ă��Ȃ�
//				if((key & 0x080000) != 0 || (key & 0x000100) != 0){//keydown
//				if((key & 0x080100) != 0){//keydown
		if(action == DOWN || keyCode == KEY_NUM8){//keydown
			if((stat & 0x0000400) != 0){	//�ݒ�
				if((stat2 & 0x0000040) != 0){//���X�ԍ��w��
					if(data[27] > 4){
						data[28] -= Powtable[data[27]-5];
						if(data[28] < 1){data[28] = 1;}
					} else if(data[27] < 4){
						data[29] -= Powtable[data[27]];
						if(data[29] < data[28]){data[29] = 0;}
					} else {
						//data[26] = 1 - data[26];
						stat2 ^= 0x0000800;
					}
				} else if((stat2 & 0x0000100) != 0){//����
					data[28] -= Powtable[data[27]];
					if((stat2 & 0x0002000) != 0 && data[28] < 1){data[28] = 1;}
					else if(data[28] < 0){data[28] = 0;}
				} else if((stat2 & 0x0000200) != 0 && data[28] > 0){
					data[28]--;
				}
				stat |= 0x8800000;	//��ʍX�V 0x8000000 + 0x0800000
			} else if( (stat & 0x0100) != 0 ){	//���X�g�{�b�N�X�\��.
				j = data[10] + data[11];

				if((stat2 & 0x0010000) != 0 && (j >= data[66] || (j == data[66]-1 && (stat2 & 0x0004000) != 0)) && (stat3 & 0x0000400) == 0){	//�u�b�N�}�[�N
					data[10] = 0;	data[11] = 0;
				} else if(j < ListBox.length - 1){	//data[10] ���X�g�ړ�	//data[11] �I���ړ�
					if(data[11] == (data[13] - 13) / (data[30] + 3) - 1){
						data[10]++;
					} else {
						data[11]++;
					}
					//stat |= 0x4000000;	//���X�g��ʍX�V
					//stat |= 0x0800000;	//�g�b�v�̍ĕ`��͕K�v�Ȃ�
					//stat |= 0x4800000;	//���̃Z�b�g
//							stat |= 0x1000000;
				} else {
					data[10] = 0;	data[11] = 0;
				}

				if(j != data[10] + data[11] && (stat2 & 0x0014000) == 0x0014000 && (stat3 & 0x0000400) == 0){	//�u�b�N�}�[�N�̈ړ�
					ChangeBookMark(j , data[10] + data[11]);
				}

				stat |= 0x4800000;	//���X�g��ʍX�V+�g�b�v�̍ĕ`��͕K�v�Ȃ�
/*					} else if(mode == 0){
			} else if(mode == 1){
				viewy -= 3;//--;
				stat |= 0x1000000;
*/
			} else if( (stat & 0x0450000) != 0 ){//���X�N���[��
			/*
				if(data[77] < DivStr.length * (data[30] + data[34]) - height + 30 ){
					if( (stat & 0x0000200) != 0 ){stat ^= 0x0008;}
					data[77] += data[35];
					stat |= 0x1000000;	//��ʍX�V
				}*/
/*
				if((data[57] & 0x00008000) != 0 && (stat & 0x0010000) != 0){
					if(data[60] + 1 < data[59]){
						//data[77] += ((CacheBrdData[nCacheIndex][data[60]].getBytes().length+2+GetDigits(nCacheTh[nCacheIndex][data[60]])) / data[42] + 1) * (data[30] + data[34]);
						data[60]++;
						data[77] = ((Linklist[data[60]] / 1000) % 10000) * (data[30] + data[34]);
						stat |= 0x1000000;	//��ʍX�V
//						System.out.println(CacheBrdData[nCacheIndex][data[60]] + " " + CacheBrdData[nCacheIndex][data[60]].getBytes().length + " " + data[42]);
					}
				} else {
*/
					if((stat & 0x0200000) == 0){
						stat3 &= ~0x0010000;	//�X�N���[������
						stat |= 0x0200000;//���X�N���[��ON
						data[88] = 0;
						//Scroll();
					}// else
					if( (data[49] & 0x20) != 0){	//SO�p��۰ُ���
						Scroll();
					}
//				}
			}
//				} else if((key & 0x020000) != 0 || (key & 0x000004) != 0){//keyup
//				} else if((key & 0x020004) != 0){//keyup
		} else if(action == UP || keyCode == KEY_NUM2){//keyup
			if((stat & 0x0000400) != 0){	//�ݒ�
				if((stat2 & 0x0000040) != 0){//���X�ԍ��w��
					if(data[27] > 4){
						data[28] += Powtable[data[27]-5];
						if(data[28] > data[26]){data[28] = data[26];}
					} else if(data[27] < 4){
						data[29] += Powtable[data[27]];
						if(data[29] > data[26]){data[29] = data[26];}
						if(data[29] < data[28]){data[29] = data[28];}
					} else {
						//data[26] = 1 - data[26];
						stat2 ^= 0x0000800;
					}
				} else if((stat2 & 0x0000100) != 0){//����
					data[28] += Powtable[data[27]];
					if(data[28] > data[26]){data[28] = data[26];}
				} else if((stat2 & 0x0000200) != 0 && data[28] < data[26]){
					data[28]++;
				}
				stat |= 0x8800000;	//��ʍX�V 0x8000000 + 0x0800000
			} else if( (stat & 0x0100) != 0 ){	//���X�g�{�b�N�X�\��
				j = data[10] + data[11];
				if(data[10] == 0 && data[11] == 0){
					if((stat2 & 0x0010000) != 0 && (stat3 & 0x0000400) == 0){	//�u�b�N�}�[�N & !�u�b�N�}�[�N���j���[
						//for(i = ListBox.length-1; i > 0; i--){
						//	if(!(BookMark[i].equals(""))){
						//		break;
						//	}
						//}
						//data[11] = i;
						if((stat2 & 0x0004000) != 0){
							data[11] = data[66] - 1;
						} else {
							data[11] = data[66];
							if(data[66] == data[40]){data[11]--;}
						}
					} else {
						data[11] = ListBox.length-1;
					}
					i = (data[13] - 13) / (data[30] + 3) - 1;
					if(data[11] > i){
						data[10] = data[11] - i;
						data[11] = i;
					}
				} else {
					if(data[11] > 0){
						data[11]--;
					} else if(data[10] > 0){
						data[10]--;
					}
					//stat |= 0x4000000;	//���X�g��ʍX�V
					//stat |= 0x0800000;	//�g�b�v�̍ĕ`��͕K�v�Ȃ�
					//stat |= 0x4800000;	//���̃Z�b�g
				}

				if(j != data[10] + data[11] && (stat2 & 0x0014000) == 0x0014000){	//�u�b�N�}�[�N�̈ړ�
					ChangeBookMark(j , data[10] + data[11]);
				}
				stat |= 0x4800000;	//���X�g��ʍX�V+�g�b�v�̍ĕ`��͕K�v�Ȃ�
/*					} else if(mode == 0){
			} else if(mode == 1){
				if(viewy < 0){viewy += 3;}//+;}
				stat |= 0x1000000;
*/
			} else if( (stat & 0x0450000) != 0 ){	//ڽ��۰�
					if((stat & 0x0100000) == 0){
						stat3 &= ~0x0010000;	//�X�N���[������
						stat |= 0x0100000;//��X�N���[��ON
						data[88] = 0;
						//Scroll();
					}// else
					if( (data[49] & 0x20) != 0){	//SO�p��۰ُ���
						Scroll();
					}
//				}
			}
//				} else if((key & 0x010000) != 0 || (key & 0x000010) != 0){//left
//				} else if((key & 0x010010) != 0){//left
		} else if(keyCode == KEY_NUM3){//pagedown
			if((stat & 0x0000100) != 0 && (stat & 0x0000400) == 0 && (stat2 & 0x0004000) == 0){//listbox
				if((stat2 & 0x0010000) != 0 && (stat3 & 0x0000400) == 0){
					i = data[66];
				} else {
					i = ListBox.length;
				}
				data[10] += (data[13] - 13) / (data[30] + 3);
				if(data[10] + data[11] >= i){
					data[10] = 0;	data[11] = 0;
					keyPressed(KEY_NUM2);
				} else if(data[10] + (data[13] - 13) / (data[30] + 3) - 1 > i){
					i = data[11];
					data[10] = 0;	data[11] = 0;
					keyPressed(KEY_NUM2);
					data[11] = i;
				}
				//j = (data[13] - 13) / (data[30] + 3);
				//for (i = 0; i < j; i++) {
			} else {
				i = (data[85] + 5/*2*/) * (data[30] + data[34]) - height;
				if(data[77] < i){
					if( (data[49] & 0x20) != 0 || (data[49] & 0x01) == 0 ){stat &= ~0x0200000;}
					data[77] += height - (data[30] + data[34]) * 2;
					if(data[77] > i){data[77] = i;}
					data[60] = -1;
					setLink();
					if(data[60] == -1){data[60] = data[59] - 1;}
				} else {data[60] = data[59] - 1;}
			}
			stat |= 0x1000000;	//��ʍX�V
		} else if(keyCode == KEY_NUM1){//pageup
			if((stat & 0x0000100) != 0 && (stat & 0x0000400) == 0 && (stat2 & 0x0004000) == 0){//listbox
				data[10] -= (data[13] - 13) / (data[30] + 3);
				if(data[10] + data[11] < 0){
					data[10] = 0;	data[11] = 0;
				} else if(data[10] < 0){
					//data[11] = data[10] + data[11];
					data[10] = 0;
				}
				//j = (data[13] - 13) / (data[30] + 3);
				//for (i = 0; i < j; i++) {
			} else {
				if(data[77] > 0 || (data[77] > -data[30] && (stat & 0x0040000) != 0) ){
					if( (data[49] & 0x20) != 0 || (data[49] & 0x01) == 0 ){stat &= ~0x0100000;}
					data[77] -= height - (data[30] + data[34]) * 2;
					if(data[77] <= -data[30] && (stat & 0x0040000) != 0){data[77] = -data[30];	data[60] = 0;}
					else if(data[77] <= 0){data[77] = 0;	data[60] = 0;}
					else {
						setLink();
					}
				} else {data[60] = 0;}
			}
			stat |= 0x1000000;	//��ʍX�V
		} else if(action == LEFT || keyCode == KEY_NUM4){
			//System.out.println(stat + "");
			if((stat4 & 0x0000100) != 0){	//7���̋@�\(�ڗ�)�ݒ�L�����Z��
				stat4 ^= 0x0000100;
				ListBox = StrList[8];
				data[10] = data[19];
				data[11] = 9 - data[19];
				stat |= 0x1000000;	//��ʍX�V
			} else if((stat4 & 0x0000400) != 0){	//7���̋@�\(�޸ϗ�)�ݒ�L�����Z��
				stat4 ^= 0x0000400;
				ListBox = StrList[8];
				data[10] = data[19];
				data[11] = 10 - data[19];
				stat |= 0x1000000;	//��ʍX�V
			} else if((stat4 & 0x0001000) != 0){	//7���̋@�\(�ڗ�)�ݒ�L�����Z��
				stat4 ^= 0x0001000;
				ListBox = StrList[8];
				data[10] = data[19];
				data[11] = 11 - data[19];
				stat |= 0x1000000;	//��ʍX�V
			}  else if((stat4 & 0x0008000) != 0){	//0���̋@�\�ݒ�L�����Z��
				stat4 ^= 0x0008000;
				ListBox = StrList[8];
				data[10] = data[19];
				data[11] = 12 - data[19];
				stat |= 0x1000000;	//��ʍX�V
			} else if((stat & 0x0000400) != 0){	//�ݒ�_�C�A���O
				if((stat2 & 0x0000040) != 0){//���X�ԍ��w��
					if(data[27] < 8){data[27]++;}
				} else if((stat2 & 0x0000100) != 0){//����
					if(data[27] < data[29]){data[27]++;}
				}
				stat |= 0x8800000;	//��ʍX�V 0x8000000 + 0x0800000
			} else if((stat3 & 0x0000400) != 0){//�ޯ�ϰ����j���[
				stat3 &= ~0x8000400;
				ListBox = tBookMark;
				data[10] = data[67];	data[11] = data[68];
				addCommand(command[0]);
				addCommand(command[3]);
				stat |= 0x1000000;	//��ʍX�V
			} else if( (stat & 0x0100) != 0 ){	//���X�g�{�b�N�X�\��
				if((stat2 & 0x0010000) != 0){	//�u�b�N�}�[�N�\��
					if( (stat & 0x0040000) != 0 ){
						ListBox = StrList[3];
						data[10] = 0;	data[11] = 0;
					} else {
						ListBox = StrList[1];
						data[10] = 0;	data[11] = 1;
					}
					stat2 ^= 0x0010000;
					//setSoftLabel(Frame.SOFT_KEY_2, null);
					removeCommand(command[3]);
					removeCommand(command[0]);
					//removeCommand(command[1]);
					//data[19] = ListBox.length;
				} else if((stat2 & 0x000003E) != 0){	//�ݒ胁�C�����j���[�ɖ߂�
					/*if((stat2 & 0x0000010) != 0){	//����ݒ�
						stat2 ^= 0x0000010;
						ListBox = StrList[4];
					} else */

					//stat2 &= ~0x0000020;
					//stat2 &= ~0x0000010;	//�ʐM�ݒ�
					//stat2 &= ~0x0000008;	//�ʐM�ݒ�
					//stat2 &= ~0x0000004;	//�F�̐ݒ�
					//stat2 &= ~0x0000002;	//�\���ݒ�
					stat2 &= ~0x000003E;

					ListBox = StrList[4];
					//data[19] = ListBox.length;
					data[10] = data[20];	data[11] = data[21];
				} else if((stat2 & 0x0000001) != 0){	//�ݒ�
					if((stat & 0x40000) != 0){	//���X�\����
						ListBox = StrList[3];
						data[10] = 0;	data[11] = 0;
					} else {
//#ifdef DEBUG
//						System.out.println("okasii? " + stat);
//#endif
						ListBox = StrList[1];
						data[10] = 0;	data[11] = 3;
					}
					//data[19] = ListBox.length;
					stat2 ^= 0x0000001;	//�ݒ����
				} else if((stat & 0x450000) != 0){	//���X�\����,�X���I��
					stat ^= 0x0000100;	//�ƭ�����
					addCommand(command[0]);
					addCommand(command[6]);
				} else if((stat & 0x04000) != 0){	//�I�𒆂̎�
					ListBox = StrList[12];
					stat |= 0x2000;	//�J�e�S���I��
					stat ^= 0x4000;	//�I�𒆉���
					removeCommand(command[7]);
					removeCommand(command[9]);
					//data[19] = ListBox.length;
					data[10] = data[22];data[11] = data[23];	//�ꏊ�̓ǂݍ���
				} else if((stat & 0x02000) != 0){	//�J�e�S���I�𒆂̎�
					ListBox = StrList[1];
					stat ^= 0x2000;	//�J�e�S���I�𒆉���
					//data[19] = ListBox.length;
					data[10] = 0;data[11] = 0;	//�ꏊ�̓ǂݍ���
//				} else if(data[10] + data[11] == 0){	//���C�����j���[�̎�
//				} else {
//					return;
				}

				stat |= 0x1000000;	//��ʍX�V
			} else if( (stat & 0x10000) != 0 ){	//�ڑI�𒆂̎�
//				if(data[5] == 0){//��Ԃ͂��߂̃X����I�����Ă����ꍇ
				if(nCacheSt[nCacheIndex] == 1){
					backfromfirst();
//				} else if(data[4] == 0){//�ǂݍ���ł钆�ň�Ԃ͂���
				} else {
//					if(data[5] <= data[0]){i = 1;} else {i = data[5] - data[0] + 1;}
					//sendstr = "b=" + data[3] + "&c=s" + (data[5] - data[0] + 1) + "t" + data[5];//m=m
					//httpinit();
					if(nCacheSt[nCacheIndex] <= data[0]){i = 1;} else {i = nCacheSt[nCacheIndex] - data[0];}
					stat3 |= 0x0000010;	//�c�k�������X�E�X���b�h���X�g���Ōォ�猩�邱�Ƃ������t���O
					stat3 |= 0x0000100;	//�Q�ƌ���ۑ����Ȃ�
					strdata[8] = strdata[9];
//					httpinit(1,nCacheBrd[nCacheIndex]/*data[3]*/,i,data[5],0);
					httpinit(1,nCacheBrd[nCacheIndex]/*data[3]*/,i,nCacheSt[nCacheIndex] - 1,0);
//				} else {
//					data[5]--;
//					data[4]--;
//
//					chkcache();	//�L���b�V���`�F�b�N
//
//					//DivStr = SeparateString(ThreadName[data[5] % data[0]], -1);
//					DivStr = FastSeparateByte(CacheBrdData[nCacheIndex]/*ThreadName*/[data[4]].getBytes());
//					data[85] = DivStr.length;
				}
			} else if( (stat & 0x0440000) != 0 && (stat2 & 0x0000080) != 0 ){	//ڽ��۰�
				if((stat2 & 0x0000080) != 0){
					if((data[49] & 0x100) != 0){
						if( data[6] > 0 ){	//�O�̃��X�ɖ߂�
							data[6]--;
							makeRes();
							//DivStr = sepalateString(ResElements[3]);
							//stat |= 0x1000000;	//��ʍX�V
						} else if(nCacheSt[nCacheIndex]/*data[7]*/ == 1) {	//��ԏ��߂̃��X
							backfromfirst();
						} else {	//�O�̃��X��ǂݍ���
							if(nCacheSt[nCacheIndex]/*data[7]*/ <= data[1]){i = 1;} else {i = nCacheSt[nCacheIndex]/*data[7]*/ - data[1];}
							//sendstr = "b=" + data[3] + "&t=" + nThread[data[5] % data[0]] + "&c=s" + i + "t" + (data[7] - 1);//m=m
							//httpinit();
							stat3 |= 0x0000010;	//�c�k�������X�E�X���b�h���X�g���Ōォ�猩�邱�Ƃ������t���O
							stat3 |= 0x0000100;	//�Q�ƌ���ۑ����Ȃ�
							strdata[8] = strdata[9];
							httpinit(2,nCacheBrd[nCacheIndex]/*data[3]*/,i,nCacheSt[nCacheIndex]/*data[7]*/ - 1,nCacheTh[nCacheIndex][0]/*data[2]*//*nThread[data[4]]*/);
						}
					}
				}
				if((stat2 & 0x0400000) == 0){
					stat3 &= ~0x0010000;	//�X�N���[������
					stat2 |= 0x0400000;//���X�N���[��ON
					//Scroll();
				}
			} else if((stat & 0x0040000) != 0){	//���X�����Ă��Ƃ�
				if( data[6] > 0 ){	//�O�̃��X�ɖ߂�
					data[6]--;
					makeRes();
					//DivStr = sepalateString(ResElements[3]);
					//stat |= 0x1000000;	//��ʍX�V
				} else if(nCacheSt[nCacheIndex]/*data[7]*/ == 1) {	//��ԏ��߂̃��X
					backfromfirst();
				} else {	//�O�̃��X��ǂݍ���
					if(nCacheSt[nCacheIndex]/*data[7]*/ <= data[1]){i = 1;} else {i = nCacheSt[nCacheIndex]/*data[7]*/ - data[1];}
					//sendstr = "b=" + data[3] + "&t=" + nThread[data[5] % data[0]] + "&c=s" + i + "t" + (data[7] - 1);//m=m
					//httpinit();
					stat3 |= 0x0000010;	//�c�k�������X�E�X���b�h���X�g���Ōォ�猩�邱�Ƃ������t���O
					stat3 |= 0x0000100;	//�Q�ƌ���ۑ����Ȃ�
					strdata[8] = strdata[9];
					httpinit(2,nCacheBrd[nCacheIndex]/*data[3]*/,i,nCacheSt[nCacheIndex]/*data[7]*/ - 1,nCacheTh[nCacheIndex][0]/*data[2]*//*nThread[data[4]]*/);
				}
			}
//				} else if((key & 0x040000) != 0 || (key & 0x000040) != 0){//right
//				} else if((key & 0x040040) != 0){//right
		} else if(action == RIGHT || keyCode == KEY_NUM6){
			if((stat & 0x0000400) != 0){	//�ݒ�_�C�A���O
				if((stat2 & 0x0000040) != 0){
					if(data[27] > 4 || (data[27] > 0 && (stat2 & 0x0000800) == 0/*data[26] == 0*/)){data[27]--;}
				} else if((stat2 & 0x0000100) != 0){//����or���X�ԍ��w��
					if(data[27] > 0){data[27]--;}
				}
				stat |= 0x8800000;	//��ʍX�V 0x8000000 + 0x0800000
			} else if((stat2 & 0x0010000) != 0 && (stat2 & 0x0040000) == 0){//�ޯ�ϰ�
				if((stat3 & 0x0000400) == 0){//�ޯ�ϰ����j���[
					//ShowBookMarkMenu();
					showBookMark(1);
				}
			} else if( (stat & 0x0440000) != 0 && (stat2 & 0x0000080) != 0){	//ڽ��۰�&AAMode�̏ꍇ
				if((stat2 & 0x0000080) != 0){
					if((data[49] & 0x100) != 0){
						if(data[6] < nCacheTo[nCacheIndex]/*data[8]*/ - nCacheSt[nCacheIndex]/*data[7]*/){	//���̃��X�ɍs��
							data[6]++;
							if(data[84] > 0 && data[6] + data[84] == nCacheTo[nCacheIndex] - nCacheSt[nCacheIndex] && nCacheTo[nCacheIndex] < nCacheAll[nCacheIndex]){
								stat3 |= 0x0000080;	//��ǂ݂����邱�Ƃ������t���O

								stat3 |= 0x0000100;	//�Q�ƌ���ۑ����Ȃ�
								strdata[8] = strdata[9];
								httpinit(2,nCacheBrd[nCacheIndex]/*data[3]*/,nCacheTo[nCacheIndex]/*data[8]*/ + 1,nCacheTo[nCacheIndex]/*data[8]*/ + data[1],nCacheTh[nCacheIndex][0]/*data[2]*//*nThread[data[4]]*/);

								//stat |= 0x0020000;	//Loading���̃X���ǂ�
							}
							makeRes();
							//DivStr = sepalateString(ResElements[3]);
							//stat |= 0x1000000;	//��ʍX�V
						} else {	//���̃��X��ǂݍ���
							//sendstr = "b=" + data[3] + "&t=" + nThread[data[5] % data[0]] + "&c=s" + (data[8] + 1) + "t" + (data[8] + data[1]);//m=m
							//httpinit();
							stat3 |= 0x0000100;	//�Q�ƌ���ۑ����Ȃ�
							strdata[8] = strdata[9];
							httpinit(2,nCacheBrd[nCacheIndex]/*data[3]*/,nCacheTo[nCacheIndex]/*data[8]*/ + 1,nCacheTo[nCacheIndex]/*data[8]*/ + data[1],nCacheTh[nCacheIndex][0]/*data[2]*//*nThread[data[4]]*/);
						}
					}
				}else{
					if((stat2 & 0x0800000) == 0){
						stat3 &= ~0x0010000;	//�X�N���[������
						stat2 |= 0x0800000;//�E�X�N���[��ON
						//Scroll();
					}
				}
			} else if((stat & 0x40000) != 0 && (stat & 0x0100) == 0){
				if(data[6] < nCacheTo[nCacheIndex]/*data[8]*/ - nCacheSt[nCacheIndex]/*data[7]*/){	//���̃��X�ɍs��
					data[6]++;
					if(data[84] > 0 && data[6] + data[84] == nCacheTo[nCacheIndex] - nCacheSt[nCacheIndex] && nCacheTo[nCacheIndex] < nCacheAll[nCacheIndex]){
						stat3 |= 0x0000080;	//��ǂ݂����邱�Ƃ������t���O

						stat3 |= 0x0000100;	//�Q�ƌ���ۑ����Ȃ�
						strdata[8] = strdata[9];
						httpinit(2,nCacheBrd[nCacheIndex]/*data[3]*/,nCacheTo[nCacheIndex]/*data[8]*/ + 1,nCacheTo[nCacheIndex]/*data[8]*/ + data[1],nCacheTh[nCacheIndex][0]/*data[2]*//*nThread[data[4]]*/);

						//stat |= 0x0020000;	//Loading���̃X���ǂ�
					}
					makeRes();
					//DivStr = sepalateString(ResElements[3]);
					//stat |= 0x1000000;	//��ʍX�V
				} else {	//���̃��X��ǂݍ���
					//sendstr = "b=" + data[3] + "&t=" + nThread[data[5] % data[0]] + "&c=s" + (data[8] + 1) + "t" + (data[8] + data[1]);//m=m
					//httpinit();
					stat3 |= 0x0000100;	//�Q�ƌ���ۑ����Ȃ�
					strdata[8] = strdata[9];
					httpinit(2,nCacheBrd[nCacheIndex]/*data[3]*/,nCacheTo[nCacheIndex]/*data[8]*/ + 1,nCacheTo[nCacheIndex]/*data[8]*/ + data[1],nCacheTh[nCacheIndex][0]/*data[2]*//*nThread[data[4]]*/);
				}
			} else if( (stat & 0x10000) != 0 ){	//���̃X���ɍs��
				if((stat2 & 0x8000000) == 0) {	//���̃X����ǂݍ���
					//sendstr = "b=" + data[3] + "&c=s" + (data[5] + 2) + "t" + (data[5] + data[0] + 1);//m=t
					//httpinit();
					stat3 |= 0x0000100;	//�Q�ƌ���ۑ����Ȃ�
					strdata[8] = strdata[9];
					//httpinit(1,nCacheBrd[nCacheIndex]/*data[3]*/,data[5] + 2,data[5] + data[0] + 1,0);
					httpinit(1,nCacheBrd[nCacheIndex]/*data[3]*/,nCacheTo[nCacheIndex] + 1,nCacheTo[nCacheIndex] + data[0],0);
				}
			}

//				} else if(/*center*/(key & 0x100000) != 0 || /*5*/(key & 0x000020) != 0 ){	//�I���{�^��
//				} else if( (key & 0x100020) != 0 ){	//�I���{�^��
		} else if(action == FIRE || keyCode == KEY_NUM5){
			if((stat & 0x0000400) != 0){	//�ݒ�
				removeCommand(command[2]);
			}
			if( (stat & 0x0100) != 0 || (stat4 & 0x0000200) != 0 || (stat4 & 0x0002000) != 0){	//���X�g�{�b�N�X�\��
				/*if((stat2 & 0x0000010) != 0){	//����ݒ�
					switch(data[10] + data[11]){
						case 4:	//�߂�
							stat2 ^= 0x0000010;
							ListBox = StrList[4];
						break;
					}
					stat |= 0x1000000;	//��ʍX�V
				} else */
				i = data[10] + data[11];
				if((stat3 & 0x0000400) != 0){//�ޯ�ϰ����j���[
					
					if((stat & 0x0000400) != 0){	//�ݒ肵����
						stat3 ^= 0x0000400;
						i = data[67] + data[68];
						strdata[8] = BookMark[i];
						setResnum(BookMarkData[i * 3], BookMarkData[i * 3 + 1]);
					}
				} else if((stat2 & 0x0010000) != 0){	//�u�b�N�}�[�N
					data[67] = data[10]; data[68] = data[11];
					j = BookMarkData[i * 3 + 2];
					if(j == 0){	//�͂��߂ւ̃u�b�N�}�[�N(1-)
						j = 1;
					}
					strdata[8] = BookMark[i];
					if(BookMarkData[i * 3 + 1] != 0){	//���X�ւ̃u�b�N�}�[�N
						//data[43] = i;
						//stat |= 0x0020000;	//���X�\����(+���X�����擾��)
						if(j == -1 || j == 9999){	//�ŐV���X�ւ̃u�b�N�}�[�N
							httpinit(2,BookMarkData[i * 3],-1,data[55],BookMarkData[i * 3 + 1]);
						} else {	//���X�ւ̃u�b�N�}�[�N
							httpinit(2,BookMarkData[i * 3],j,j + data[1] - 1,BookMarkData[i * 3 + 1]);
						}
					} else if(!(BookMark[i].length() == 0)){	//�X���ꗗ(��)�ւ̃u�b�N�}�[�N
						//data[43] = i;
						//stat |= 0x8000;	//�X���ꗗ�c�k��
						httpinit(1,BookMarkData[i * 3],j,j + data[0] - 1,0);
					}
				} else if((stat4 & 0x0000100) != 0){	//7���̋@�\(�ڗ�)�ݒ��
					stat4 ^= 0x0000100;
					data[94] = data[10]+data[11];
					ListBox = StrList[8];
					data[10] = data[19];
					data[11] = 9 - data[19];
					stat |= 0x1000000;	//��ʍX�V
					Bugln("�ڗ�\n");
				} else if((stat4 & 0x0000400) != 0){	//7���̋@�\(�޸ϗ�)�ݒ��
					stat4 ^= 0x0000400;
					data[71] = data[10]+data[11];
					ListBox = StrList[8];
					data[10] = data[19];
					data[11] = 10 - data[19];
					stat |= 0x1000000;	//��ʍX�V
					Bugln("�޸ϗ�\n");
				} else if((stat4 & 0x0001000) != 0){	//7���̋@�\(�ڗ�)�ݒ��
					stat4 ^= 0x0001000;
					data[73] = data[10]+data[11];
					ListBox = StrList[8];
					data[10] = data[19];
					data[11] = 11 - data[19];
					stat |= 0x1000000;	//��ʍX�V
					Bugln("�ڗ�\n");
				}  else if((stat4 & 0x0008000) != 0){	//0���̋@�\�ݒ��
					stat4 ^= 0x0008000;
					data[74] = data[10]+data[11];
					ListBox = StrList[8];
					data[10] = data[19];
					data[11] = 12 - data[19];
					stat |= 0x1000000;	//��ʍX�V
					Bugln("0��\n");
				} else if((stat2 & 0x0000020) != 0){	//���̑�
					othermenu(i);
					stat |= 0x1000000;	//��ʍX�V
				} else if((stat2 & 0x0000008) != 0){	//�ʐM�ݒ�
					networksetting(i);
					stat |= 0x1000000;	//��ʍX�V
/*						} else if((stat2 & 0x0000004) != 0){	//�F�̐ݒ�
					switch(data[10] + data[11]){
						case 5:	//�߂�
							stat2 ^= 0x0000004;
							ListBox = SettingMenu;
							//data[19] = ListBox.length;
							data[10] = data[11] = 0;
						break;
					}
					stat |= 0x1000000;	//��ʍX�V
*/
				} else if((stat2 & 0x0000004) != 0){	//����ݒ�
					contmenu(i);
					stat |= 0x1000000;	//��ʍX�V
				} else if((stat2 & 0x0000002) != 0){	//�\���ݒ�
					//i = data[10] + data[11];
					viewmenu(i);
					stat |= 0x1000000;	//��ʍX�V
				} else if((stat2 & 0x0000001) != 0){	//�ݒ�̎�
					//data[19] = ListBox.length;
					settingmenu(i);
					data[20] = data[10];	data[21] = data[11];
					data[10] = 0;data[11] = 0;
					stat |= 0x1000000;	//��ʍX�V
				//} else if((stat & 0x0000200) != 0){	//�f�[�^�t�H���_�̕\��
					//-/ReadDataFolder(data[10] + data[11]);
					//-/stat |= 0x0400000;	//�f�[�^�t�H���_�̕\��

					//-/stat &= ~0x0100;//if((stat & 0x0100) != 0){stat ^= 0x0100;}
					//-/data[6] = 0;
					//-/addCommand(command[0]);//���j���[
					//-/stat |= 0x1000000;	//��ʍX�V
				} else if( (stat & 0x40000) != 0 || (stat4 & 0x0002000) != 0){	//ڽ���Ă鎞
					if((stat4 & 0x0002000) != 0){
						i = data[73];
						stat4 ^= 0x0002000;
					}
					strdata[8] = strdata[9];
					resmenu(i,j);
					stat |= 0x1000000;	//��ʍX�V
				} else if( (stat & 0x10000) != 0 || (stat4 & 0x0000200) != 0){	//�ڑI�𒆂̎�
					if((stat4 & 0x0000200) != 0){
						i = data[94];
						stat4 ^= 0x0000200;
					}
					strdata[8] = CacheBrdData[nCacheIndex]/*ThreadName*/[data[60]];
					threadmenu(i);
					stat |= 0x1000000;	//��ʍX�V
				} else if((stat & 0x04000) != 0){	//�I�𒆂̎�
					//stat ^= 0x4000;	//�I�𒆉���
					//stat |= 0x8000;	//�X���ꗗ�c�k��
				//	data[3] = (data[22] + data[23]) * 100 + i;
					strdata[8] = ListBox[i];
					/*
					sendstr = "b=" + data[3];//m=t
					if(data[0] != 10){
						sendstr = sendstr + "c=s1t" + data[0];
					}
					httpinit();*/
					httpinit(1,(data[22] + data[23]) * 100 + i/*data[3]*/,1,data[0],0);

				} else if((stat & 0x02000) != 0){	//�J�e�S���I�𒆂̎�
/*
					while(data[44] <= i){
						try{Thread.sleep(100);}catch (Exception e){}
					}
*/
					ListBox = split( StrList[13][i], '\t');
					stat ^= 0x2000;	//�J�e�S���I�𒆉���
					stat |= 0x4000;	//�I��
					stat |= 0x1000000;	//��ʍX�V
					data[22] = data[10];data[23] = data[11];	//�ꏊ�̕ۑ�
					//data[19] = ListBox.length;
					data[10] = 0;data[11] = 0;
					addCommand(command[9]);
					addCommand(command[7]);
				} else {	//���C�����j���[�̎�
					//i = data[10] + data[11];
					mainmenu(i);
					if(i == 0 || i == 3 ){
						stat |= 0x1000000;	//��ʍX�V
						//data[19] = ListBox.length;
						data[10] = 0;data[11] = 0;
					}
				}
/*					} else if(mode == 0){
			} else if(mode == 1){
*/
			} else if( (stat & 0x10000) != 0 ){	//�ڑI�𒆂̎�
				strdata[8] = CacheBrdData[nCacheIndex][data[60]];
				if((data[57] & 0x00010000) != 0){	//�ŐV���X��ǂސݒ�̏ꍇ
					httpinit(2,nCacheBrd[nCacheIndex]/*data[3]*/,-1,data[55],nCacheTh[nCacheIndex]/*nThread*/[data[60]]);
					stat |= 0x40000000;	//Loading���̑���͕s�\
				} else {
					httpinit(2,nCacheBrd[nCacheIndex]/*data[3]*/,1,data[1],nCacheTh[nCacheIndex][data[60]]);
				}
			} else if( (stat & 0x40000) != 0 ){	//���X�̕\��
				Link(null, 0);
			}
			if((stat & 0x0000400) != 0){	//�ݒ�
				addCommand(command[2]);
			}

		} else if(keyCode == KEY_NUM7){
			if( (stat & 0x10000) != 0){	//�ڑI�𒆂̎�
				stat4 |= 0x0000200;
				keyPressed(KEY_NUM5);
			}

			if( (stat & 0x0040000) != 0){	//ڽ�I�𒆂̎�
				stat4 |= 0x0002000;
				keyPressed(KEY_NUM5);
			}
			
			if((stat2 & 0x0010000) != 0){//�ޯ�ϰ�
				if(/*BookMarkData[i * 3 + 1] == 0 && */BookMark[(data[10]+data[11])].length() == 0){	//�X���ԍ���0�@���o�^����Ă���or��

				}else{
					if(BookMarkData[(data[10]+data[11]) * 3 + 1] == 0){	//��
						
					}else{
						stat4 |= 0x0080000;
						data[67] = data[10];	data[68] = data[11];
						data[10] = 0;	data[11] = 0;
						stat3 |= 0x0000400;
						keyReleased(KEY_NUM5);
					}
				}
				
			}

		} else if(keyCode == KEY_STAR){
			if( (stat & 0x0004000) != 0 ){	//���X�g�ɂ���Ƃ�
				strdata[7] = "�ԍ�:" + ((data[22] + data[23]) * 100 + data[10] + data[11]);//StrList[10][7] + StrList[10][19];
				stat2 |= 0x0001000;
			} else if((stat & 0x0000100) == 0/*���X�g�{�b�N�X�͔�\��*/ /*(stat2 & 0x0000F0F) == 0*//*!=�ݒ���*/){
				if((stat & 0x40000) != 0/*���X�ǂݒ�*/){
					if( (stat2 & 0x0000080) != 0 ){	//AA MODE
						stat2 ^= 0x0000080;
					} else {
						stat2 |= 0x0000080;	//AA MODE
					}
					makeRes();
					//stat |= 0x1000000;	//��ʍX�V
				} else if((stat & 0x10000) != 0 ){	//�ڈꗗ�̕\��
					data[92]++;
					if(data[92] == 3){data[92] = 0;}
					makeTL();
				}
			} else {
				stat2 |= 0x0004000;
			}
			stat |= 0x1000000;	//��ʍX�V
/*

*/
		} else if(keyCode == KEY_POUND){	//#
			//if((data[57] & 0x00000100) != 0){data[57] &= ~0x00000100;	ColScm = ColPreset[0];
			//} else {data[57] |= 0x00000100;	ColScm = ColPreset[1];}
			if(data[58] < ColPreset.length - 1){
				data[58]++;
			} else if(data[58] == ColPreset.length - 1){
				data[58] = 0;
			}
			ColScm = ColPreset[data[58]];
			stat |= 0x1000000;	//��ʍX�V
		}else if(keyCode == KEY_NUM0){ //0�L�[�V���[�g�J�b�g
			orezimenu(data[74]);
		}

		thttpget();	//httpgetdata()��V�K�X���b�h�œ���

	}
	/**
	 * ���X�w��p�{�b�N�X�̌Ăяo������у��X�w�菈���̎��s
	 * @param i ��Ɍ��݂̃��X�ԍ����w�肷�鏉���l
	 * @param j �����l(�O��)
	 */
	public final void setResnum(int i, int j){
		if((stat & 0x0000400) != 0){	//�ݒ肵����
			stat ^= 0x0000400;
			stat2 ^= 0x0000040;//0x0000100;	//����
			stat2 ^= 0x0002000;
			zuzu[0] = data[28];
			zuzu[1] = data[29];
			if((stat2 & 0x0000800) != 0/*data[26] == 1*/){
				httpinit( 2, i, zuzu[0], zuzu[0], j/*nThread[data[4]]*/);
			} else if(data[29] == 0){
				httpinit( 2, i, zuzu[0], zuzu[0] + data[1] - 1, j/*nThread[data[4]]*/);
			} else {
				httpinit( 2, i, zuzu[0], zuzu[1], j/*nThread[data[4]]*/);
			}
		} else {
			//NumSet();
			removeCommand(command[9]);
			removeCommand(command[6]);
			stat |= 0x0000400;
			stat2 |= 0x0000040;	//���X�ԍ��w�胂�[�h
			//data[26] = 0;	//0:-�L�� 1:-����
			if(i >= 1000000){
				data[26] = 9999;
			} else {
				data[26] = 1000;
			}
			stat2 &= ~0x0000800;	//0:-�L�� 1:-����
			data[27] = 5;	//�I�����Ă��錅
			if(zuzu[0] != 0){
				data[28] = zuzu[0];	//�����l(�O��)
				data[29] = zuzu[1];	//�����l(�㔼)
			}else{
				data[28] = j;	//�����l(�O��)
				data[29] = 0;	//�����l(�㔼)
			}
			strdata[2] = StrList[3][2];
		}
	}
	/**
	 * �ʐM�����B�ꗗ�̎擾�ȊO�̏�����httpgetdata()�ւƔ�΂��B
	 * @see MainCanvas#httpgetdata()
	 */

	public final synchronized void thttpget(){

		if( (stat & 0x0010) != 0 && (stat3 & 0x0000800) == 0){	//�ʐM��
			stat3 |= 0x0000800;		//�ʐM�@�\�g�p��
			Thread thread = new Thread() {
				public final void run() {
					if((stat3 & 0x0100000) != 0){	//�ꗗ�̎擾
						stat3 &= ~0x0100000;
						stat |= 0x0010;	//�ʐM
						httpgetdata();
						if((stat2 & 0x0001000) == 0){	//�G���[���b�Z�[�W���o�Ă��Ȃ��Ƃ�
							try {
								brdarray = dlarray;
								//DataOutputStream sp = new DataOutputStream(Connector.openOutputStream("scratchpad:///0;pos=" + 21010));
								//sp.writeUTF(new String(canv.dlarray));
								//sp.close();
								RecordStore recordStore = RecordStore.openRecordStore("Brdlist", true);
								if(recordStore.getNumRecords() == 0){
									recordStore.addRecord(brdarray, 0, brdarray.length);
								} else {
									recordStore.setRecord( 1, brdarray, 0, brdarray.length);
								}
								recordStore.closeRecordStore();//��
							} catch(Exception e){}
							brdinit();
							strdata[7] = "�ꗗ�擾����";
							stat2 |= 0x0001000;
							stat |= 0x1000000;	//��ʍX�V
						}
					} else {
						httpgetdata();
					}
				}
			};
			thread.start();
		}
/*
		Thread thread = new Thread() {
			public final void run() {
				if((stat3 & 0x0100000) != 0){	//�ꗗ�̎擾
					stat3 &= ~0x0100000;
					//stat |= 0x0001000;	//�ꗗ�擾��
					//data[78] = 3;	//��ʂ̃_�E�����[�h
					data[78] = 0x00000008;	//��ʂ̃_�E�����[�h
					stat |= 0x0010;	//�ʐM
					httpgetdata();
					if((stat2 & 0x0001000) == 0){	//�G���[���b�Z�[�W���o�Ă��Ȃ��Ƃ�
						try {
							DataOutputStream sp = new DataOutputStream(Connector.openOutputStream("scratchpad:///0;pos=" + 21010));
							sp.writeUTF(new String(dlarray));
							sp.close();
						} catch(Exception e){}
						brdinit();
						strdata[7] = "�ꗗ�擾����";
						stat2 |= 0x0001000;
					}
					//stat &= ~0x0001000;	//�ꗗ�擾��
				} else if((stat3 & 0x0200000) != 0){	//�L�[���s�[�g
					stat3 &= ~0x0200000;
					int k = data[83];//key;

					//keyPressed(k);
					while((stat2 & 0x2000000) != 0 && (getKeypadState() & k) != 0){
						keyPressed(k);
						try{
							sleep(data[82]*3);
						}catch (Exception e){}
					}
					keyth = null;
					stat2 ^= 0x2000000;
					//stat2 &= ~0x4000000;
				} else if( (stat & 0x0010) != 0 && (stat3 & 0x0000800) == 0){	//�ʐM��
					httpgetdata();
				}
			}
		};
		thread.start();
*/
	}
	//�L�[�����[�X
	/**
	 * �L�[�����[�X
	 */
	protected synchronized void keyReleased(int keyCode) {
		if( keyCode != 0 ){
			int action = getGameAction(keyCode);


			//if(((stat & 0x0300000) != 0 || (stat2 & 0x0C00000) != 0) && (action & (DOWN | KEY_NUM8 | UP | KEY_NUM2 | LEFT | KEY_NUM4 | RIGHT | KEY_NUM6)) != 0 && (stat3 & 0x0010000) == 0/*�X�N���[������*/){
			//	Scroll();
			//}
			stat &= ~0x0300000;//�㉺�X�N���[��OFF
			stat2 &= ~0x0C00000;//���E�X�N���[��OFF
			//if(action == DOWN || keyCode == KEY_NUM8){//keydown
			//	stat &= ~0x0200000;//���X�N���[��OFF
			//} else if(action == UP || keyCode == KEY_NUM2){//keyup
			//	stat &= ~0x0100000;//��X�N���[��OFF
			//} else if(action == LEFT || keyCode == KEY_NUM4){
			//	stat2 &= ~0x0400000;//���X�N���[��OFF
			//} else if(action == RIGHT || keyCode == KEY_NUM6){
			//	stat2 &= ~0x0800000;//�E�X�N���[��OFF
			//} else
			if(action == FIRE || keyCode == KEY_NUM5){
				int i = data[10] + data[11];
				int j = data[67] + data[68];
				int k = 0;
				if((stat3 & 0x0000400) != 0){//�ޯ�ϰ����j���[
					if((stat3 & 0x8000000) != 0){//�u�b�N�}�[�N�̓��ʃ��j���[�\��
						i += 7;
					} else {
						if(BookMarkData[j * 3 + 1] == 0) {	//�X���ԍ���0�@���o�^����Ă���or��
							//if(BookMark[j].equals("")/*BookMark[j].getBytes().length == 0*/){	//��
								//if(i == 0){i = 5;}
								//else{i += 6;}
							//} else {	//��
								//if(i == 0){i = 2;} else if(i == 1){i = 3;} else {i = 4;}
								i += 4;
							//}
						}
					}
					if((stat4 & 0x0080000) != 0){
						i = data[71];
						stat4 ^= 0x0080000;
					}

					strdata[8] = BookMark[j];
					bookmarkmenu(i,j);
					if(i == 1){
						addCommand(command[2]);
					} else {
						//stat3 ^= 0x0000400;
						stat3 &= ~0x8000400;
						ListBox = tBookMark;
						data[10] = data[67];	data[11] = data[68];
						addCommand(command[0]);
						addCommand(command[3]);
					}
					stat2 &= ~0x0004000;	//function����
					stat |= 0x1000000;	//��ʍX�V
				}
/*			} else if(keyCode == KEY_NUM7){
				System.gc();
				Runtime runtime = Runtime.getRuntime();
				//strdata[7] = null;
				StrList[15] = new String[3];
				StrList[15][0] = "��؏��";
				StrList[15][1] = "free:" + runtime.freeMemory();
				StrList[15][2] = "total:" + runtime.totalMemory();
				stat2 |= 0x0001000;
				stat |= 0x1000000;	//��ʍX�V
*/
			} else if(keyCode == KEY_NUM9){//�p�P�b�g�\�������ǂ݂ɕύX
				//if( (stat & 0x50000) != 0 ){	//ڽ���Ă鎞or�ڑI�𒆂̎�
					//viewcost();
					stat3 |= 0x0000080;	//��ǂ݂����邱�Ƃ������t���O
	
					stat3 |= 0x0000100;	//�Q�ƌ���ۑ����Ȃ�
					strdata[8] = strdata[9];
					httpinit(2,nCacheBrd[nCacheIndex]/*data[3]*/,nCacheTo[nCacheIndex]/*data[8]*/ + 1,nCacheTo[nCacheIndex]/*data[8]*/ + data[1],nCacheTh[nCacheIndex][0]/*data[2]*//*nThread[data[4]]*/);

					stat |= 0x1000000;	//��ʍX�V
				//}
			} else if(keyCode == KEY_NUM0){


			}
			//if((stat & 0x0000004) != 0){	//�L�[�X�g�b�v���������Ă�����
			//	return;
			//}
			if(keyCode == KEY_STAR){
				stat2 &= ~0x0004000;
				stat |= 0x1000000;	//��ʍX�V
			}
		}
	}
	/**
	 * ���C���̕`�揈���B�����I�ɂ͂����𕪊�����paintTL��paintRes��paintMenu�ɕ��������B
	 */
	public synchronized final void paint(Graphics g/*raphics*/) {
		try{

			stat3 |= 0x1000000;	//�`�撆
			int i, j, k, byo = 0;
			int sc = data[77];	//�X�N���[���̒l�̃e���|����(�������_�u���̖h�~)
			int _stat;	//�X�e�[�^�X�i�R�s�[�j
			String boxstr[];
			//stat |= 0x1000000;
//			System.out.println("g");
/*
			if( (stat2 & 0x0000040) != 0){
				return;
			}
*/
			if((data[49] & 0x02) != 0){//�b�\��
				byo = 2;
			}

			if((data[57] & 0x00000008) != 0){
				if(byo == 0){byo = 5;} else {byo += 6;}
			}
			_stat = stat;
			g.setFont(font);
			if( (_stat & 0xF000000) != 0 ){//1+2+4+8
				stat &= ~0xF000000;	//�ĕ`��t���O����

//				stat2 |= 0x0000040;//�`�惍�b�N
				//Graphics g = getGraphics();
				//g.lock();
				if( (_stat & 0xD000000) != 0 ){//13=1+4+8
					if( (_stat & 0x1000000) != 0 ){	//�S�̂��ĕ`��
						g.setColor(ColScm[0],ColScm[1],ColScm[2]);//2ch�̃X���w�i�F 239,239,239
						g.fillRect(0, data[30] + 3, width, height);

						if( (_stat & 0x0450000) != 0){// || (stat & 0x40000) != 0 ){	//�ڈꗗ�̕\�� or ���X�̕\��
//							g.setColor(ColScm[3],ColScm[4],ColScm[5]);	//0,0,0
//							if( (_stat & 0x10000) != 0 ){	//�ڈꗗ�̕\��
								//if(nRes[data[5] % data[0]] >= 1000){i = 3;}else if(nRes[data[5] % data[0]] >= 100){i = 2;}else if(nRes[data[5] % data[0]] >= 10){i = 1;}else {i = 0;}
//								i = GetDigits(nCacheBrdData[nCacheIndex][data[4]]/*nRes[data[4]]*/);

//								g.drawString(nCacheBrdData[nCacheIndex][data[4]]/*nRes[data[4]]*/ + "ڽ", width - 6 - (i + 2) * data[33], data[12] - 1 - data[32], 68/*g.BASELINE|g.LEFT*/);
//								i = data[5] - data[4] + 1;
//								g.drawString("��" + (data[5] + 1) + "/" + i + "-" + (i + nCacheTh[nCacheIndex]/*nThread*/.length - 1), 6, data[12] - 1 - data[32], 68/*g.BASELINE|g.LEFT*/);
								//g.setColor(0,0,255);
					//		} else if( (_stat & 0x0400000) != 0 ){	//�f�[�^�t�H���_�̕\��
					//			g.drawString("test" + (sc / (data[30] + data[34]))  + " " + DivStr.length + " " + DivStr[0], 0, 40, 20/*g.TOP|g.LEFT*/);//���O
//							}
							//���X�̕\��
							j = data[30] + data[34];//����+�s��
							i = sc / j;	if(0 > i){i = 0;}	//DivStr�̏����n��
							k = (height + sc - data[30] - 4) / j + 1;	//����ȏ�͕\������Ȃ��̂ŏ����Ȃ�
							if(k > data[85]){k = data[85];}	//index����͂���Ȃ����߂�
							//�����N�̏���F�h��
							//if( (_stat & 0x40000) != 0 ){	//���X�̕\��
							int n, m, o, p;//, q;
//							data[60] = -1;
//							q = data[65];
							for(n = 0;n < data[59];n++){
								m = (Linklist[n] / 1000) % 10000;
								if(m >= i || m <= k){	//�����N����Ƃ���͕\�����ɂ���ꍇ
									//System.out.println("m:" + m + " i:" + i + " sc:" + sc + " 62:" + data[60] + " n:" + n + " q:" + q);
//									if(data[60] == -1 && ((sc == -data[30] && m == 0)/*��s��*/ || (m - i) > 0/*�B��Ă���ꍇ�̓t�H�[�J�X��������Ȃ�*/)){
//										if(q == 0){	//skip�����Ȃ��ꍇ
//											//System.out.println("focus:" + n);
//											g.setColor(ColScm[24],ColScm[25],ColScm[26]);	//�����N�̐F(focus)
//											data[60] = n - data[65];
//										} else {	//����ꍇ
//											q--;
//											g.setColor(ColScm[27],ColScm[28],ColScm[29]);	//�����N�̐F
//										}
									if((data[57] & 0x00004000) != 0 && nCacheInfo[n] == 1){
										if(data[60] == n){
											g.setColor(ColScm[30],ColScm[31],ColScm[32]);	//�����N�̐F(focus,cached)
										} else {
											g.setColor(ColScm[33],ColScm[34],ColScm[35]);	//�����N�̐F(cached)
										}
									} else {
										if(data[60] == n){
											g.setColor(ColScm[24],ColScm[25],ColScm[26]);	//�����N�̐F(focus)
										} else {
											g.setColor(ColScm[27],ColScm[28],ColScm[29]);	//�����N�̐F
										}
									}
									o = (Linklist[n] / 1000) / 10000;
									if((stat2 & 0x0000080) == 0){	//AA MODE�ł͂Ȃ��ꍇ
										//System.out.println("linklist:" + Linklist[n] + " n:" + n);
										g.fillRect(data[48] + Linklist[n] % 1000 * data[33], 4 + (m + 1/*����1��1�s�ڗp*/) * j - sc, Math.min(o, data[42] - Linklist[n] % 1000) * data[33], data[30]);
										o -= data[42] - Linklist[n] % 1000;
										for(p = 1;o > 0;p++){
											g.fillRect(data[48], 4 + (m + 1/*����1��1�s�ڗp*/ + p) * j - sc, Math.min(o, data[42]) * data[33], data[30]);
											o -= data[42];
										}
									} else{
										g.fillRect(data[48] + Linklist[n] % 1000 * data[33], 4 + (m + 1/*����1��1�s�ڗp*/) * j - sc, o * data[33], data[30]);
									}
								}
							}
							if( 1 > sc && (_stat & 0x40000) != 0 ){	//�P�s�ڂ̖��O����\������ꍇ
								//i = GetDigits(data[6] + data[7]) - 1;

								g.setColor(ColScm[3],ColScm[4],ColScm[5]);	//�����̐F 0,0,0
								g.drawString((data[6] + nCacheSt[nCacheIndex]/*data[7]*/) + ":", data[48], 4 - sc, 20/*g.TOP|g.LEFT*/);
								g.setColor(ColScm[18],ColScm[19],ColScm[20]);//ڽ�̖��O green
								g.drawString(strdata[3]/*ResElements[0]*/, (GetDigits(data[6] + nCacheSt[nCacheIndex]/*data[7]*/) + 1) * data[33] + data[48], 4 - sc, 20/*g.TOP|g.LEFT*/);//���O
							}
							//}

							g.setColor(ColScm[3],ColScm[4],ColScm[5]);	//�����̐F 0,0,0
							sc -= 4 + j;	//�g�b�v�̗]��1+���C��2+�]��1+1�s�ڗp
							if( (_stat & 0x0040000) != 0){	//���X�̕\��
								if(i == 0){
									g.drawSubstring(resstr, 0, iDivStr[0], data[48], i * j - sc, 20);	i++;
								}
								for (; i < k; i++) {
									g.drawSubstring(resstr, iDivStr[i-1], iDivStr[i] - iDivStr[i-1], data[48], i * j - sc, 20);
								}
							} else {
								for (; i < k; i++) {
									//if(16 + i * j - sc > height){break;}
									g.drawString(DivStr[i], data[48], i * j - sc, 20/*g.TOP|g.LEFT*/);
								}
							}
							sc += 4 + j;	//�g�b�v�̗]��1+���C��2+�]��1+1�s�ڗp
						}
					}
					if( (_stat & 0x5000000) != 0 && (_stat & 0x0100) != 0 ){		//List Box
						g.setColor(ColScm[0],ColScm[1],ColScm[2]);//2ch�̃X���w�i�F 239,239,239
						g.fillRect(8, data[12] + 2, width - 17, data[13] - 6);
	/*						if( (_stat & 0x4000000) != 0 ){		//List Box�ĕ`��
							g.fillRect(10, data[12] + 2, width - 20, data[13] - 7);
						} else {
							g.fillRect(10, data[12] + 2, width - 21, data[13] - 8);
						}
	*/
						if((stat2 & 0x0004000) != 0){
							g.setColor(ColScm[15],ColScm[16],ColScm[17]);	//ؽđI��F(Function) 255,165,0
						} else {
							g.setColor(ColScm[12],ColScm[13],ColScm[14]);	//ؽđI��F 192,192,255
						}
						g.fillRect(12, data[12] + 7 + data[11] * (data[30] + 3), width - 26, data[30]);	//�I��\��
						if(wallpaper != null && data[12] == data[30] + 3 && (data[57] & 0x00000400) == 0){
							//g.drawImage(wallpaper,9 + data[37],data[12] + data[38], 20/*g.TOP|g.LEFT*/ );//���i�[�̊G
							g.drawImage(wallpaper,data[37],/*data[12] + */data[38], 20/*g.TOP|g.LEFT*/ );//���i�[�̊G
						}
						g.setColor(ColScm[3],ColScm[4],ColScm[5]);	//�����̐F 0,0,0
						j = (data[13] - 13) / (data[30] + 3);
						for (i = 0; i < j; i++) {
							if(i + data[10] >= ListBox.length){break;}
							if(ListBox[i + data[10]] != null){
								g.drawString(ListBox[i + data[10]], 12, data[12] + 7 + i * (data[30] + 3), 20/*g.TOP|g.LEFT*/);
							}
							if(/*data[12] + */5 + (i + 1) * (data[30] + 3) < /*data[12] + */data[13] - 8){	//��؂��������
								g.setColor(ColScm[9],ColScm[10],ColScm[11]);	//���Ԃ̔Z�� 128,128,128
								g.drawLine(12, data[12] + 5 + (i + 1) * (data[30] + 3), width - 15, data[12] + 5 + (i + 1) * (data[30] + 3));
								g.setColor(ColScm[3],ColScm[4],ColScm[5]);	//�����̐F 0,0,0
							}
						}
						g.setColor(ColScm[3],ColScm[4],ColScm[5]);	//�����̐F 0,0,0
						if(ListBox.length > (data[13] -13)  / (data[30] + 3) + data[10]){	//�����
							DrawTriangle(g, width / 2, data[12] + data[13] - 8, 0);
						}
						if(data[10] > 0){	//����
							DrawTriangle(g, width / 2, data[12] + 1, 1);
						}
						//if( (_stat & 0x4000000) == 0 ){		//List Box�̍ĕ`��łȂ��ꍇ(�S�̂��ĕ`��)
						if( (_stat & 0x1000000) != 0 ){		//�S�̂��ĕ`��
							//�g
							g.setColor(ColScm[9],ColScm[10],ColScm[11]);	//���Ԃ̔Z�� 128,128,128
							g.drawLine(8, data[12] + 1, width - 10, data[12] + 1);//��
							g.drawLine(8, data[12] + data[13] - 4, width - 10, data[12] + data[13] - 4);//��
							g.drawLine(7, data[12] + 2, 7, data[12] + data[13] - 5);//��
							g.drawLine(width - 9, data[12] + 2, width - 9, data[12] + data[13] - 4/*- 5*/);//�E
							g.setColor(ColScm[6],ColScm[7],ColScm[8]);	//�����F 192,192,192
							//g.drawLine(width - 8, 18, width - 8, height - 4);
							g.fillRect(width - 8, data[12] + 3, 3, data[13] - 4);
							g.fillRect(10, data[12] + data[13] - 3, width - 18, 2);
						}
					}
					if( (_stat & 0x9000000) != 0 ){
						if((_stat & 0x0000400) != 0 && (stat2 & 0x0000340) != 0){	//�ݒ�_�C�A���O && ����or�������[�h
							k = 0;
							if((stat2 & 0x1001000) != 0){	//������\��or�C���t�H���[�V�����\��	0x0001000 || 0x1000000
								k = data[30] * -2;
							}
							if((stat2 & 0x0000040) != 0){//���X�ԍ��w�胂�[�h
								i = GetDigits(data[28]);
								strdata[6] = "";
								for(j = 0;j < 4 - i;j++){
									strdata[6] = strdata[6] + "0";
								}
								String str = strdata[2] + ":" + strdata[6] + data[28];
								//if(data[26] == 0){
								if((stat2 & 0x0000800) == 0){
									i = GetDigits(data[29]);

									strdata[6] = "";
									for(j = 0;j < 4 - i;j++){
										strdata[6] = strdata[6] + "0";
									}
									str += "-" + strdata[6] + data[29];
								} else {
									str += "     ";
								}

								i = DrawBoxStr( g, data[50], data[51]+k, null, str, 4) + data[33] / 2;
							} else if((stat2 & 0x0000100) != 0){//����
//									if(data[26] >= 1000){i = 4;}else if(data[26] >= 100){i = 3;}else if(data[26] >= 10){i = 2;}else {i = 1;}
								//if(data[28] >= 1000){i = 4;}else if(data[28] >= 100){i = 3;}else if(data[28] >= 10){i = 2;}else {i = 1;}
								i = GetDigits(data[28]);
								strdata[6] = "";
								for(j = 0;j < data[29] + 1 - i;j++){
									strdata[6] = strdata[6] + "0";
								}
								//i = DrawLineStr( g, data[50], data[51], strdata[2] + ":" + strdata[6] + "" + data[28], 4) + data[33] / 2;
								i = DrawBoxStr( g, data[50], data[51]+k, null, strdata[2] + ":" + strdata[6] + data[28], 4) + data[33] / 2;
							} else {//�ꕶ��
								//i = DrawLineStr( g, data[50], data[51], strdata[2] + ":" + StrList[14][data[28]], 4);
								i = DrawBoxStr( g, data[50], data[51]+k, null, strdata[2] + ":" + StrList[14][data[28]], 4);
								/*
								if(data[28] < data[26]){
									DrawTriangle(g, i - data[33] - 1, height / 2 - data[33] - 7, 1);//ue
								}
								if(data[28] > 0){
									DrawTriangle(g, i - data[33] - 1, height / 2 + data[33], 0);//sita
								}*/
							}
							if((stat2 & 0x0000040) == 0 || data[27] > 4){
								j = data[28];
							} else if(data[27] < 4) {
								j = data[29];
							} else {
								j = -1;
							}
							if(j < data[26] || ((stat2 & 0x0000040) != 0 && j < 1000) || j == -1){
								DrawTriangle(g, i - data[33] * (data[27] + 1)/* - 1*/, data[51] - data[33] - 7 + k, 1);//ue
							}
							if(j > 1 || ((stat2 & 0x0002000) == 0 && data[28] > 0) || j == -1){
								DrawTriangle(g, i - data[33] * (data[27] + 1)/* - 1*/, data[51] + data[33] + k, 0);//sita
							}
//							} else if((stat2 & 0x0000400) != 0){	//�F�ݒ胂�[�h
//								DrawLineStr( g, width / 2, height / 2 - (data[30] + 10), strdata[2], 0);
//								DrawLineStr( g, width / 2, height / 2, "R:   G:   B:", 3);
						}
					}
					//stat &= ~0x1000000;//�ĕ`��OFF
					//if( (stat & 0x1000000) != 0 ){	//�ĕ`��OFF
					//	stat ^= 0x1000000;
					//}
				}
				if( (_stat & 0x5000000) != 0 || (_stat & 0x2000000) == 0){	//�g�b�v�����X�V���Ȃ��Ƃ��͕`�悵�Ȃ��B
					if(data[56] > 1 && (_stat & 0x0000100) == 0 && (_stat & 0x0010000) != 0 && nCacheTh[nCacheIndex][data[60]] > 900000000){	//�X���I��
						calendar.setTime(new Date((long)nCacheTh[nCacheIndex][data[60]] * 1000));
						if(data[92] == 1){
							boxstr = new String[3];
							boxstr[0] = "ڽ:" + nCacheBrdData[nCacheIndex][data[60]];
							i = height - (data[30] + data[34]) * 3 / 2 - 5;
							j = 1;
						} else {
							boxstr = new String[2];
							i = height - (data[30] + data[34]) - 5;
							j = 0;
						}
						boxstr[j] = "since:" + (calendar.get(1/*Calendar.YEAR*/)) + "/" + (calendar.get(2/*Calendar.MONTH*/)+1) + "/" + calendar.get(5/*Calendar.DATE*/);
						boxstr[j+1] = "����:" + nCacheBrdData[nCacheIndex][data[60]] * 86400 /(System.currentTimeMillis()/ 1000 - nCacheTh[nCacheIndex][data[60]]) + "ڽ/��";
						//if(data[60] > nCacheTh[nCacheIndex].length / 2){
						//if(((Linklist[data[60]] / 1000) % 10000 + 1/*����1��1�s�ڗp*/) * (data[30] + data[34]) - sc > height / 2){
						//	i = data[30] + (data[30] + data[34]) + 7;
						//} else {
						//}
						DrawBoxStr( g, width - data[33] * 9, i, boxstr, null, 0);
					}
					if((stat2 & 0x1001000) != 0){	//������\��or�C���t�H���[�V�����\��	0x0001000 || 0x1000000
						//if(strdata[7] != null){
						//	DrawLineStr( g, data[50], data[51], strdata[7], 0);
						//} else {
						//	DrawBoxStr( g, data[52], data[53], StrList[15]);
						//}
						k = 0;
						if((_stat & 0x0000400) != 0 && (stat2 & 0x0000340) != 0){	//�ݒ�_�C�A���O && ����or�������[�h
							k = data[30] + 5;
						}
						DrawBoxStr( g, data[52], data[53]+k, StrList[15], strdata[7], 0);
					} else if((_stat & 0x0000004) != 0){	//�L�[�X�g�b�v���������Ă�����
						if((_stat & 0x0020000) != 0){
							i = width - data[33] * 6 + 3; j = height - data[33] - 5;
							//DrawLineStr( g, width - data[33] * 6 + 3, height - data[33] - 5, "Loading�c"/*StrList[10][10]*/, 0);
						} else {
							i = width / 2; j = height / 2;
							//DrawLineStr( g, width / 2, height / 2, "Loading�c"/*StrList[10][10]*/, 0);
						}
						DrawBoxStr( g, i, j, null, "Loading�c"/*StrList[10][10]*/, 0);
					}
				}
				if((_stat & 0x0800000) == 0 || (_stat & 0x1000000) != 0){	//�g�b�v�̍ĕ`��͕K�v�Ȃ����Ƃ͂Ȃ��ꍇ
					//�g�b�v
					g.setColor(ColScm[0],ColScm[1],ColScm[2]);//2ch�̃X���w�i�F 239,239,239
					g.fillRect(0, 0, width, data[30] + 2);
					//g.setColor(ColScm[3],ColScm[4],ColScm[5]);	//�����̐F 0,0,0
					//data[39] = data[31]/* - 1*/;
					if( (_stat & 0x00DF010) == 0 && (stat2 & 0x001000F) == 0/* && (stat2 & 0x0000020) != 0*/){	//�^�C�g����ʂ̏ꍇ
						g.setColor(0, 0, 255);	//blue
						g.drawString( "iMona@zuzu "/* + StrList[10][0]*/ + "v" + version, 0, 1, 20/*g.TOP|g.LEFT*/);

					}
					g.setColor(ColScm[3],ColScm[4],ColScm[5]);	//�����̐F 0,0,0

					/*if( (stat & 0x00E0) == 0x0080 ){	//�ʐM�G���[
						g.drawString("\ue6d1", 30, data[31]);//i�}�[�N
						g.drawString("�~", 30, data[31]);//�~�}�[�N
					} else */
	//				if( (stat & 0x10000) != 0 || (stat & 0x40000) != 0 ){	//�ڈꗗ�̕\�� or ���X�̕\��
					k = width - data[33] * byo - 4;
					if((_stat & 0x0000010) == 0/*�ʐM���ł͂Ȃ�*/ || byo == 0/*�b�\��*/){
						if(byo == 0){
							k += 2;
						}
						if( (_stat & 0x50000) != 0 ){	//�ڈꗗ�̕\�� or ���X�̕\��
							if((_stat & 0x0000010) == 0 && (stat & 0x10000000) == 0){
								//�X�����E���̕\��
								if(strdata[9] != null){
									g.drawString(strdata[9], 1, 1, 20/*g.TOP|g.LEFT*/);//�E�X���̖��O
								}
								/*
								if( (_stat & 0x40000) != 0 ){//���X�̕\��
									if(ThreadName[data[4]] != null){
										g.drawString(ThreadName[data[4]], 1, data[31]+1, g.BASELINE|g.LEFT);//�X���b�h��
									}
								} else {
									if(strdata[9] != null){
										g.drawString(strdata[9], 1, data[31]+1, g.BASELINE|g.LEFT);//�̖��O
									}
								}
								*/
								g.setColor(ColScm[0],ColScm[1],ColScm[2]);//2ch�̃X���w�i�F 239,239,239
								g.fillRect(k - data[33] * 8 - 4, 0, data[33] * (8+byo) + 9, data[30]);
								g.setColor(ColScm[3],ColScm[4],ColScm[5]);	//�����̐F 0,0,0
							}
							if( (_stat & 0x10000) != 0 ){	//�ڈꗗ�̕\��
								//i = data[4];
								//j = CacheBrdData[nCacheIndex]/*ThreadName*/.length - 1;
								i = j = -1;
							} else if( (_stat & 0x40000) != 0 ){	//���X�̕\��
								i = data[6];
								j = nCacheTo[nCacheIndex]/*data[8]*/ - nCacheSt[nCacheIndex]/*data[7]*/;
							} else {i = 0;j = 0;}
							if( (stat2 & 0x0000080) != 0 ){	//AA MODE
								k += - data[33] * 8 + 7;
								g.setColor(ColScm[21],ColScm[22],ColScm[23]);	//���ӐF
								g.drawString("AA MODE", k, 1, 20/*g.TOP|g.LEFT*/);
								//g.setColor(ColScm[3],ColScm[4],ColScm[5]);	//�����̐F 0,0,0
							} else {
								k += - data[33] * 4;
								if(i < j){g.drawString("����", k, 1, 20/*g.TOP|g.LEFT*/);}
								else {
									if((stat3 & 0x0800000) != 0){
										g.drawString("�~��", k, 1, 20/*g.TOP|g.LEFT*/);
									} else {
										g.setColor(ColScm[21],ColScm[22],ColScm[23]);	//���ӐF
										g.drawString("�ځ�", k, 1, 20/*g.TOP|g.LEFT*/);
										g.setColor(ColScm[3],ColScm[4],ColScm[5]);	//�����̐F 0,0,0
									}
								}
								k += - data[33] * 4 - 3;
								if(i > 0){g.drawString("��O", k, 1, 20/*g.TOP|g.LEFT*/);
								//} else if( ((_stat & 0x40000) != 0) && nCacheSt[nCacheIndex]/*data[7]*/ == 1){
								//	g.drawString("��X", k, data[31]+1, 68/*g.BASELINE|g.LEFT*/);
								//} else if( ((_stat & 0x10000) != 0) && nCacheSt[nCacheIndex] == 1/*data[5] == 0*/){
								//	g.drawString("���", k, data[31]+1, 68/*g.BASELINE|g.LEFT*/);
								} else if( ((_stat & 0x50000) != 0) && nCacheSt[nCacheIndex] == 1){
									g.drawString("���", k, 1, 20/*g.TOP|g.LEFT*/);
								} else {
									if((stat3 & 0x0400000) != 0){
										g.drawString("��~", k, 1, 20/*g.TOP|g.LEFT*/);
									} else {
										g.setColor(ColScm[21],ColScm[22],ColScm[23]);	//���ӐF
										g.drawString("���", k, 1, 20/*g.TOP|g.LEFT*/);
										//g.setColor(ColScm[3],ColScm[4],ColScm[5]);	//�����̐F 0,0,0
									}
								}
							}
							g.setColor(ColScm[3],ColScm[4],ColScm[5]);	//�����̐F 0,0,0
							if((_stat & 0x0000100) == 0 && (_stat & 0x0450000) != 0  && (data[57] & 0x00004000) != 0 && data[60] >= 0 && nCacheInfo[data[60]] == 1){
								g.setColor(ColScm[0],ColScm[1],ColScm[2]);//2ch�̃X���w�i�F 239,239,239
								g.fillRect(k - data[33]*3, 0, data[33]*3, data[30]);
								g.setColor(ColScm[3],ColScm[4],ColScm[5]);	//�����̐F 0,0,0
								g.drawString("[C]", k - data[33]*3, 1, 20/*g.TOP|g.LEFT*/);
								k -= data[33]*3;
							//	DrawBoxStr( g,  data[33] * 4 + 2, height - data[33] - 5, null, "������L"/*StrList[10][10]*/, 0);
							}
							//g.drawString((i + 1) + "/" + (j + 1), 30, data[31]);
							//g.drawString((data[6] + data[7]) + "/" + data[9], 30, data[31]);
						} else if( (_stat & 0x0002000) != 0 || (_stat & 0x0004000) != 0 || (stat2 & 0x001000F) != 0/* || (stat3 & 0x0000400) != 0 */){	//�J�e�S���I�� or �I�� or �ݒ� or �u�b�N�}�[�N(���j���[)�̕\����
							//k = width - data[33] * (8+byo) - 3 - byo;
		//				} else if( (stat & 0x100000) != 0 ){	//�ʐM�\��
		//					g.drawString("\ue6d1", 0, data[31]);//i�}�[�N
							if((_stat & 0x0000010) == 0){	//�ʐM���ł͂Ȃ��Ƃ�
								if((stat3 & 0x0000400) != 0){	//�u�b�N�}�[�N(���j���[)�̕\����
									g.drawString( BookMark[data[67]+data[68]], 0, 1, 20/*g.TOP|g.LEFT*/);
								} else {
									g.setColor(ColScm[21],ColScm[22],ColScm[23]);	//���ӐF
									if( (_stat & 0x0002000) != 0){
										g.drawString( "�ú��", 0, 1, 20/*g.TOP|g.LEFT*/);
									} else if( (_stat & 0x0004000) != 0 ){
										g.drawString( "�� - " + StrList[12][data[22]+data[23]], 0, 1, 20/*g.TOP|g.LEFT*/);
									} else if((stat2 & 0x0010000) != 0 ){	//�u�b�N�}�[�N
										g.drawString( "�ޯ�ϰ�", 0, 1, 20/*g.TOP|g.LEFT*/);
									} else if((stat2 & 0x0000001) != 0){	//�ݒ�̎�
										g.drawString( "�ݒ�", 0, 1, 20/*g.TOP|g.LEFT*/);
									}
								}
							}
							g.setColor(ColScm[0],ColScm[1],ColScm[2]);//2ch�̃X���w�i�F 239,239,239
							if( (stat2 & 0x0010000) != 0 && (stat3 & 0x0000400) == 0 ){	//�u�b�N�}�[�N�̕\����
								k += - data[33] * 9;
								g.fillRect(k, 0, data[33] * 9 + 2, data[30]);
								g.setColor(ColScm[3],ColScm[4],ColScm[5]);	//�����̐F 0,0,0
								g.drawString("��O Me��", k, 1, 20/*g.TOP|g.LEFT*/);
							} else {
								k += - data[33] * 4;
								g.fillRect(k, 0, data[33] * 4 + 2, data[30]);
								g.setColor(ColScm[3],ColScm[4],ColScm[5]);	//�����̐F 0,0,0
								g.drawString("��O", k, 1, 20/*g.TOP|g.LEFT*/);
							}
						}
					}
					if( (stat & 0x10000000) != 0 || (_stat & 0x0000010) != 0 ){	//�_�E�����[�h�̐i�s�\���o�[
						g.setColor(ColScm[6],ColScm[7],ColScm[8]);	//�����F 192,192,192
						g.drawRect(0, 0, k-1, data[30] - 1);
						if(data[16] != 0 && data[18] != 0){
							g.setColor(ColScm[12],ColScm[13],ColScm[14]);	//ؽđI��F 192,192,255
							g.fillRect(1, 1, (k-2) * data[16] / data[18], data[30] - 2);
						}
						g.setColor(ColScm[3],ColScm[4],ColScm[5]);	//�����̐F 0,0,0
						if(data[16] == 0){
							//if(StrList[10] != null)
								g.drawString("�ڑ���"/*StrList[10][8]*/, 1, 0, 20/*g.TOP|g.LEFT*/);//�ڑ���
						} else if(data[16] == data[18]){
							//if(StrList[10] != null)
								g.drawString("������"/*StrList[10][9]*/, 1, 0, 20/*g.TOP|g.LEFT*/);//������
						} else if(data[18] != 0){
							//datasize = "" + boardsize;
							g.drawString(data[16] + "/" + data[18], 1, 0, 20/*g.TOP|g.LEFT*/);//�ʐM��
						}
					//} else if( (_stat & 0x50000) != 0 ){	//�ڈꗗ�̕\�� or ���X�̕\��
					}
					//��؂�o�[
					g.setColor(ColScm[3],ColScm[4],ColScm[5]);	//�����̐F 0,0,0
					g.drawLine(0,data[30] + 3 - 1,width-1,data[30] + 3 - 1);
					g.setColor(ColScm[9],ColScm[10],ColScm[11]);	//���Ԃ̔Z�� 128,128,128
					g.drawLine(0,data[30] + 3 - 2,width-1,data[30] + 3 - 2);
					//�g�b�v�̕`��I���
				} else {	//�g�b�v�̍ĕ`��͕K�v�Ȃ�
					stat &= ~0x0800000;	//�g�b�v�̍ĕ`��͕K�v�Ȃ�������
				}
	//				g.drawString("m:" + Runtime.getRuntime().freeMemory() + "/" + Runtime.getRuntime().totalMemory(), 0, 50);
				//g.unlock(true);
				//stat &= ~0xF000000;	//�ĕ`��t���O����
				//if( (stat & 0x2000000) != 0 ){
				//	stat ^= 0x2000000;
				//}
////				graphics.drawImage( img2, 0, 0, 20/*g.TOP|g.LEFT*/);	//��ʂɓ]��
//				stat2 ^= 0x0000040;//�`�惍�b�N����
			}

			if(byo != 0){//�b�\��
				calendar.setTime(new Date(System.currentTimeMillis()));

				//�h��Ԃ�
				g.setColor(ColScm[0],ColScm[1],ColScm[2]);//2ch�̃X���w�i�F 239,239,239
				g.fillRect(width - data[33] * byo - 2, 0, data[33] * byo + 2, data[30] + 1);

				if( (_stat & 0x7000000) != 0 ){
					//��؂��
					g.setColor(ColScm[9],ColScm[10],ColScm[11]);	//���Ԃ̔Z�� 128,128,128
					g.drawLine( width - data[33] * byo - 3, 0, width - data[33] * byo - 3, data[30]);
				}

				g.setColor(ColScm[3],ColScm[4],ColScm[5]);	//�����̐F 0,0,0
				if(byo == 2 || byo >= 7){
					//�b�̕\��
					g.drawString((calendar.get(Calendar.SECOND) / 10) + "" + (calendar.get(Calendar.SECOND) % 10) + "", width-1, 1, 24/*g.TOP|g.RIGHT*/);
					if(byo == 8){
						g.drawString(":", width-1-data[33] * 2, 1, 24/*g.TOP|g.RIGHT*/);
					}
				}

				if(byo >= 5){
					//���E���̕\��
					//if(calendar.get(Calendar.HOUR_OF_DAY) < 10){byo--;}
					g.drawString(calendar.get(Calendar.HOUR_OF_DAY) + ":" + (calendar.get(Calendar.MINUTE) / 10) + "" + (calendar.get(Calendar.MINUTE) % 10), width-1-data[33] * (byo - 5), 1, 24/*g.TOP|g.RIGHT*/);
				}
			}



			if (data[91] > 0 && data[91] < data[47] / 100){	//�p�P�b�g�オ�ݒ肳�ꂽ���z�𒴂��Ă���ꍇ
				//g.setColor(ColScm[21],ColScm[22],ColScm[23]);	//���ӐF
				DrawBoxStr( g, data[52], height - 10 - data[30], null, "�߹�đ�x��!!(\\" + (data[47] / 100) + ")", 0);
			}
			if(data[95] == -1){
				DeviceControl dc = DeviceControl.getDefaultDeviceControl();
				DrawBoxStr( g, data[52], height - 10 - data[30]*3, null, "\uE00A" + dc.getDeviceState(dc.BATTERY) + "% \uE20B" + dc.getDeviceState(dc.FIELD_INTENSITY) + "%", 0);
			}
		} catch(Exception e){
		}
		stat3 &= ~0x1000000;	//�`�撆����
	}

	/**
	 * String�̃g���~���O�����B��Ƀu�b�N�}�[�N�n����Ă΂��B
	 * @param str �g���~���O������������
	 * @param w �g���~���O�������ő吔�l
	 * @return String
	 */
	public final String trimstr(final String str, int w){
		if(str.getBytes().length > w / data[33]){
			byte[] b = str.getBytes();
			int n = 0;
			w = w / data[33] - 1;
			for(int i = 0; i < b.length; i++){
				if(0 > b[i] && (b[i] <= -97 || -32 <= b[i])){//SJIS�̂P�o�C�g��(�������[�h(����~�X���N���邩��))
					i++;
				}
				if(i >= w){break;}
				n++;
			}
			return str.substring(0, n) + "..";
			//data[42] = w / data[33] - 1;
			//String[] stmp = FastSeparateByte(str.getBytes());
			//data[42] = width / data[33];	//��s�̕���(byte)��
			//return stmp[0] + "..";
		}

		return str;
	}
	//mode == 0 : �ʏ퓮��
	//mode > 1  : �����N�̃L���b�V���`�F�b�N(�����N�̉ӏ���mode - 1)

	/**
	 * �����N���J�����߂̏���
	 * @param url �����N�ŊJ�������A�h���X���w��
	 * @param mode 0�̏ꍇ�͒ʏ퓮��A1�����傫���ꍇ�̓����N�̃L���b�V���`�F�b�N(�����N�̉ӏ���mode - 1)
	 */
	public final void Link(String url, int mode){
		int i, j;
		if(url == null){
			int link = data[60]/* + data[65]*/;
			if(link == -1){	//link
				return;
			}
			if(mode > 0){
				link = mode -1;
			}
			int n, to = -1;//, m = Linklist[link] / 1000;

//			j = m / 10000;	//�����N�̕�����
			//byte[] b = DivStr[(Linklist[link] / 1000) % 10000].getBytes();
			url = Linkurllist[link];
			byte[] b = url.getBytes();
			if(b[0] == 62 || b[0] == 129){	//���X�������N
				j = 1;	n = 0;
				for(i = b.length - 1/*(Linklist[link] / 1000) / 10000 - 1*/;i > 0;i--){
					if(b[i] != 62 && b[i] != 132){
						if(b[i] == 45){	//-
							to = n;	n = 0;	j = 1;
						} else {
							n += (b[i] - 48) * j;
							j = j * 10;
						}
					}
				}
				//from = n;
				if(n > 0){
					if(to == -1){
						to = n;//from;// + data[1] - 1;
					} else if(to > n/*from*/ + data[1] - 1){
						to = n/*from*/ + data[1] - 1;
					}

					/*
					if(data[7] <= from && from <= data[8]){
						data[6] = from - data[7];
						makeRes();
						stat |= 0x1000000;	//��ʍX�V
					} else {
						httpinit(2,data[3],from,to,nThread[data[4]]);
					}*/
					strdata[8] = strdata[9];
					if(mode > 0){
						httpinit( 12, nCacheBrd[nCacheIndex]/*data[3]*/, n/*from*/, to, nCacheTh[nCacheIndex][0]/*data[2]*//*nThread[data[4]]*/);
					} else {
						httpinit( 2, nCacheBrd[nCacheIndex]/*data[3]*/, n/*from*/, to, nCacheTh[nCacheIndex][0]/*data[2]*//*nThread[data[4]]*/);
					}
				}
				return;
			}
/*			 else {	//URL
				Link(url);
/*
				j = url.indexOf(".") + 1;
				//System.out.println("<" + url + ">" + url.indexOf("2ch.net", k + 1) + " " + k + " " + url.indexOf("bbspink.com", k + 1));
				if(url.indexOf("2ch.net", j) == j || url.indexOf("bbspink.com", j) == j){
					strdata[6] = url;
					httpinit(5, 0, 1, 0, 0);
				} else {
					url = url.substring(7);
					//}
					openbrowser(url);
				}
*/
		}
		if(mode > 0){return;}

		j = url.indexOf(".") + 1;
		//System.out.println("<" + url + ">" + url.indexOf("2ch.net", k + 1) + " " + k + " " + url.indexOf("bbspink.com", k + 1));
		//URL�W�����v

		if(url.indexOf("www.2ch.net", j) == j || url.indexOf("www-2ch.net", j) == j || url.indexOf("2ch.net/ad.htm", j) == j || url.indexOf("mup.vip2ch.com") != -1 || url.indexOf("wktk.vip2ch.com") != -1 || url.indexOf("c-docomo.2ch.net") != -1){
			url = url.substring(7);
			//}
			openbrowser(url,0);
		}else if(url.indexOf("jbbs.livedoor.jp") != -1){ //������΂�URL�Ǎ�
			int br, sp, ep, w;
			String p , s, t , d;
			s = "0";
			t = "auto";
			if(url.indexOf("auto") != -1){
				t = "auto";
				s = "10";
			}else if(url.indexOf("computer") != -1){
				t = "computer";
				s = "11";
			}else if(url.indexOf("game") != -1){
				t = "game";
				s = "12";
			}else if(url.indexOf("movie") != -1){
				t = "movie";
				s = "13";
			}else if(url.indexOf("music") != -1){
				t = "music";
				s = "14";
			}else if(url.indexOf("shop") != -1){
				t = "shop";
				s = "15";
			}else if(url.indexOf("sports") != -1){
				t = "sports";
				s = "16";
			}else if(url.indexOf("travel") != -1){
				t = "travel";
				s = "17";
			}else if(url.indexOf("ness") != -1){
				t = "ness";
				s = "18";
			}else if(url.indexOf("study") != -1){
				t = "study";
				s = "19";
			}else if(url.indexOf("news") != -1){
				t = "news";
				s = "20";
			}else if(url.indexOf("otaku") != -1){
				t = "otaku";
				s = "21";
			}else if(url.indexOf("anime") != -1){
				t = "anime";
				s = "22";
			}else if(url.indexOf("comic") != -1){
				t = "comic";
				s = "23";
			}else if(url.indexOf("school") != -1){
				t = "school";
				s = "24";
			}
			//d = "jbbs.livedoor.jp/" + t + "/";
			//sp = url.indexOf(d);
			//sp = sp + d.length();
			sp = url.lastIndexOf('/') + 1;
			ep = url.length();
			p = url.substring( sp, ep);
			w = p.length();
			for (i = w; i < 5; i++) {
			    p = "0" + p;
			}
			//System.out.println(t);
			br = Integer.parseInt(s + p);
			//br.parseInt(s + p);//s + p
			stat2 = 0x0010000;
			httpinit(1, br, 1, data[0], 0);
			//url.indexOf()
			//url.substring();
			//t = s.;
			return;
		} else if(url.indexOf("2ch.net", j) == j || url.indexOf("bbspink.com", j) == j || url.indexOf("kakiko.com", j) == j || url.indexOf("vip2ch.com", j) == j || url.indexOf("jbbs.livedoor.jp") != -1){
			strdata[6] = url;
			//System.out.println(url);
			httpinit(5, 0, 1, 0, 0);
			//thttpget();	//httpgetdata()��V�K�X���b�h�œ���
		} else {
			url = url.substring(7);
			//}
			openbrowser(url,0);
		}
	}
	/**
	 * �u���E�U�N������
	 * @param url �u���E�U�ŊJ�������A�h���X���w��
	 * @param mode 0�̏ꍇ�͒ʏ퓮��A1�����傫���ꍇ�̓����N�̃L���b�V���`�F�b�N(�����N�̉ӏ���mode - 1)
	 */
	public final void openbrowser(String url, int mode){
		try {
			if( (stat4 & 0x0010000) != 0 ){	//0�����s
				mode = 1;
			}
			if(url.indexOf("http://") == 0){
				url = url.substring(7,url.length());
			}
			if(mode == 0){
				BrowserConnection conn = (BrowserConnection)Connector.open( "url://" + cushionlink + url);
				conn.connect();
				conn.close();
			}else if(mode == 1){
				//System.out.println(url);
				BrowserConnection conn = (BrowserConnection)Connector.open( "url://" + url );
				conn.connect();
				conn.close();

			}
			stat |= 0x1000000;	//��ʍX�V

		} catch ( Exception e ) {// �G���[�����B
		}
	}
	/**
	 * ���l�w��p�{�b�N�X�̕\���p�t���O����������
	 */
	public final void NumSet(){
		stat2 &= ~0x0000F00;

		stat |= 0x0000400;
		stat2 |= 0x0002100;	//���������
		//stat2 |= 0x0000100;	//�����ݒ�
		//stat2 |= 0x0002000;	//�����ݒ�ł̍ł��Ⴂ������1�ɂ���t���O
		data[26] = 1000;	//���
		data[27] = 0;	//�I�����Ă��錅
		data[29] = 3;	//�I���o���鐔���̏���̌���-1
	}
//	public final void backfromthreadlist(){	//�X���b�h�ꗗ����(�ꗗ�A�u�b�N�}�[�N)�ɖ߂�
	/**
	 * �X���b�h�ꗗ�̂͂��߂܂��̓X���b�h��>>1����(�ꗗ�A�X���b�h�ꗗ�A�u�b�N�}�[�N)�ɖ߂鏈���B
	 * �܂��ԈႢ�Ȃ��o�O����B
	 */
	public final void backfromfirst(){	//�X���b�h�ꗗ�̂͂��߂܂��̓X���b�h��>>1����(�ꗗ�A�X���b�h�ꗗ�A�u�b�N�}�[�N)�ɖ߂�
		if( (stat & 0x10000) != 0 ){	//�ڑI�𒆂̎�
			if((stat3 & 0x0000020) != 0/*(stat2 & 0x0020000) != 0*/){	//�u�b�N�}�[�N����A�N�Z�X���Ă��ꍇ
				stat3 ^= 0x0000020;
				showBookMark(0);
			} else {	//�I����ʂɖ߂�
				ListBox = split( StrList[13][nCacheBrd[nCacheIndex]/*data[3]*/ / 100/*data[22] + data[23]*/], '\t');
				stat |= 0x4000;	//�I��
				removeCommand(command[6]);
				removeCommand(command[0]);
				addCommand(command[9]);
				addCommand(command[7]);
				//data[19] = ListBox.length;
				if(data[24] + data[25] != nCacheBrd[nCacheIndex]/*data[3]*/ % 100){
					data[10] = nCacheBrd[nCacheIndex]/*data[3]*/ % 100;data[11] = 0;	//�ꏊ�̓ǂݏo��
				} else {
					data[10] = data[24];data[11] = data[25];	//�ꏊ�̓ǂݏo��
				}
				if(data[22] + data[23] != nCacheBrd[nCacheIndex]/*data[3]*/ / 100){
					data[22] = nCacheBrd[nCacheIndex]/*data[3]*/ / 100;data[23] = 0;	//�ꏊ�̓ǂݏo��
				}
				stat |= 0x0000100;
				stat |= 0x1000000;	//��ʍX�V
			}
			stat &= ~0x10000;	//�ڑI�𒆉���
			stat2 &= ~0x8000000;	//�������ʕ\�����t���O
			//data[12] = data[30] + 3;			//LIST Y���W
			//data[13] = height - (data[30] + 3);	//LIST �c��
		} else if((stat & 0x0040000) != 0){	//���X�����Ă��Ƃ�
			if((stat2 & 0x0020000) != 0){	//�u�b�N�}�[�N����A�N�Z�X���Ă��ꍇ(���ڃX���b�h�ւ̃u�b�N�}�[�N)
				stat2 &= ~0x0020000;
				//data[77] = 0;
				stat &= ~0x00040000;	//�X�������Ă�t���O������
				showBookMark(0);
			} else {	//�X���I����ʂɖ߂�
				int i = 1;
				if(data[64] > 0){	//�Q�ƌ�������ꍇ
					for(i = data[64];i > 0;i--){
						if(Linkref[i*3+1] == 0 && Linkref[i*3] == nCacheBrd[nCacheIndex]){
							stat3 |= 0x0000040;	//�߂�ŃA�N�Z�X���Ă���
							data[64] = i;
							i = Linkref[i*3+2];
							break;
						}
						if(i == 1){
							data[64] = 0;
							//i = 1;
							break;
						}
					}
				}
				//System.out.println("i:" + i);
				stat3 |= 0x0000100;	//�Q�ƌ���ۑ����Ȃ�
				httpinit( 1, nCacheBrd[nCacheIndex]/*data[3]*/, i, 0, 0);
				//if((stat3 & 0x0000200) != 0){//�Q�ƌ���ۑ����Ă����ꍇ
				//	data[64]--;	//���O�ɕۑ������Q�ƌ���j������
				//}
			/*
				ListBox = StrList[2];	data[12] = height - 13 - (data[30] + 3) * data[56];	data[13] = 13 + (data[30] + 3) * data[56];
				//DivStr = SeparateString(ThreadName[data[5] % data[0]], -1);//sepalateString(ThreadName[data[5] % data[0]]);
				DivStr = FastSeparateByte(ThreadName[data[4]].getBytes());
				data[85] = DivStr.length;
				stat |= 0x10000;
				*/
			}
		}
	}
	/**
	 * �u�b�N�}�[�N�ƃu�b�N�}�[�N���j���[�\������
	 * @param mode 0�̏ꍇ�͕\���A1�ꍇ�̓��j���[��\��
	 */
	public final void showBookMark(int mode){
		if(mode == 0){	//�u�b�N�}�[�N�̕\��
			//if( (stat & 0x40000) != 0 ){
			//removeCommand(command[0]);
			removeCommand(command[6]);
			removeCommand(command[9]);
			//}
			removeCommand(command[0]);
			//data[19] = ListBox.length;
			if((stat2 & 0x0010000) == 0){	//�u�b�N�}�[�N���\������Ă��Ȃ�
				stat2 |= 0x0010000;	//�u�b�N�}�[�N�̕\��
	//			data[10] = 0;	data[11] = 0;
				data[10] = data[67]; data[11] = data[68];
				addCommand(command[0]);
				addCommand(command[3]);
				//addCommand(command[1]);
				//ListBox = new String[BookMark.length];//BookMark;
			}
			ListBox = tBookMark;
			stat |= 0x0000100;	//���X�g�{�b�N�X�̕\��
			//stat |= 0x1000000;	//��ʍX�V
		} else {	//�u�b�N�}�[�N�̃��j���[�̕\��
			int i = data[10]+data[11];
			//removeCommand(command[0]);
			removeCommand(command[3]);
			removeCommand(command[0]);
			if(/*BookMarkData[i * 3 + 1] == 0 && */BookMark[i].length() == 0/*BookMark[i].getBytes().length == 0*/ || (stat2 & 0x0004000) != 0) {	//�X���ԍ���0�@���o�^����Ă���or��
//#ifdef 	//
/*				ListBox = new String[5];
				ListBox[0] = "�ҏW";
				ListBox[1] = "���Ԃ��l�߂�";
				ListBox[2] = "���Ԃ����";
				ListBox[3] = "����߰�";
				ListBox[4] = "���߰�";
				*/
				ListBox = StrList[16];
//#else
//				ListBox = new String[2];
//				ListBox[0] = "�ҏW";
//				ListBox[1] = "���Ԃ��l�߂�";
//#endif
				//i = 2;
				stat3 |= 0x8000000;
			} else {	//�X���b�h
				if(BookMarkData[i * 3 + 1] == 0){	//��
//#ifdef 	//
//					if((stat2 & 0x0004000) != 0){	//function
//						ListBox = new String[7];
//					} else {
//						ListBox = new String[3];
//					}
//#else
//					ListBox = new String[3];
//#endif
//					i = 0;
					ListBox = StrList[18];
				} else {
//#ifdef 	//
//					if((stat2 & 0x0004000) != 0){	//function
//						ListBox = new String[11];
//					} else {
//						ListBox = new String[7];
//					}
//#else
//					ListBox = new String[7];
//#endif
					//ListBox[0] = "�ҏW";
					//ListBox[1] = "�گ�ނ̌���";
					//ListBox[2] = "����&�I��";
					//ListBox[3] = "�ŐVڽ��ǂ�";
					//ListBox[4] = "����";
//					ListBox[0] = "�ŐVڽ��ǂ�";
//					ListBox[1] = "ڽ�Ԏw��";
//#ifdef 	//java appli
//					ListBox[2] = "����";
//#else
//					ListBox[2] = "����&�I��";
//#endif
//					ListBox[3] = "������ʂ�URL";
//					i = 4;
					ListBox = StrList[17];
				}
//				ListBox[i] = "�گ�ތ���";	i++;
//				ListBox[i] = "�ҏW";	i++;
//				ListBox[i] = "����";	i++;

//#ifdef 	//
//				if((stat2 & 0x0004000) != 0){	//function
//					ListBox[i] = "���Ԃ��l�߂�";	i++;
//					ListBox[i] = "���Ԃ����";	i++;
//					ListBox[i] = "����߰�";	i++;
//					ListBox[i] = "���߰�";	i++;
//				}
//#endif
			}
			data[67] = data[10];	data[68] = data[11];
			data[10] = 0;	data[11] = 0;
			stat3 |= 0x0000400;
			//stat |= 0x1000000;	//�ĕ`��
		}
		stat |= 0x1000000;	//��ʍX�V
	}
/*
	public final void ShowBookMarkMenu(){
	}
*/
	/**
	 * �u�b�N�}�[�N�̈ʒu����
	 * @param n1 �����Ώۈʒu
	 * @param n2 ������ʒu
	 */
	public final void ChangeBookMark(int n1,int n2){//�u�b�N�}�[�N�̈ʒu������
		int i;
		String str;

		str = BookMark[n1];
		BookMark[n1] = BookMark[n2];
		BookMark[n2] = str;
		str = tBookMark[n1];
		tBookMark[n1] = tBookMark[n2];
		tBookMark[n2] = str;

		i = BookMarkData[n1 * 3];
		BookMarkData[n1 * 3] = BookMarkData[n2 * 3];
		BookMarkData[n2 * 3] = i;
		i = BookMarkData[n1 * 3+1];
		BookMarkData[n1 * 3+1] = BookMarkData[n2 * 3+1];
		BookMarkData[n2 * 3+1] = i;
		i = BookMarkData[n1 * 3+2];
		BookMarkData[n1 * 3+2] = BookMarkData[n2 * 3+2];
		BookMarkData[n2 * 3+2] = i;
		if(n1 >= data[66] || n2 >= data[66] && data[66] != data[40]){
			data[66]++;
		}
		SaveBookMark(n1);
		SaveBookMark(n2);
		//showBookMark();
	}
	/**
	 * �u�b�N�}�[�N�̕ҏW
	 * @param mode 0:add 0>:del 0<:�㏑��
	 * @param str �o�^���镶����
	 * @param b �ԍ�
	 * @param t �X���b�h�ԍ�
	 * @param r ���X�ԍ�
	 * @return �߂�l -1:���s 0<=:����(�o�^���ꂽ�ʒu)
	 */
	//�ޯ�ϰ��̕ҏW		�߂�l -1:���s 0<=:����(�o�^���ꂽ�ʒu)
	public final int EditBookMark(int mode/*0:add 0>:del 0<:�㏑��*/,String str/*�o�^���镶����*/, int b, int t, int r){
		int i;
		if(mode == 0){
			for(i = 0;i < BookMark.length;i++){
				//if(BookMarkData[i * 3 + 1] == 0 && BookMarkData[i * 3] == 0){//thread & board are 0
				if(BookMarkData[i * 3 + 1] == 0 && BookMark[i].equals("")/*BookMark[i].getBytes().length == 0*/){	//thread = 0 && title = ""
					mode = i + 1;
					strdata[7] = "�o�^����";//StrList[10][6] + StrList[10][19];
					stat2 |= 0x0001000;
					break;
				}
			}
		}
		if(mode < 0){	//del
			mode = - 1 - mode;
			strdata[7] = "�폜����";//StrList[10][7] + StrList[10][19];
			stat2 |= 0x0001000;
			BookMark[mode] = "";//"<���g�p>";
			tBookMark[mode] = "";//"<���g�p>";
			//if((stat2 & 0x0010000) != 0){	//�u�b�N�}�[�N���\������Ă���
			//	showBookMark();
			//}
			BookMarkData[mode * 3] = 0;
			BookMarkData[mode * 3 + 1] = 0;
			BookMarkData[mode * 3 + 2] = 0;
			SaveBookMark(mode);
			if(mode == data[66] - 1){
				data[66]--;
			}
			stat |= 0x1000000;	//��ʍX�V
			return mode;
		} else if(mode > 0){	//add or replace
			mode--;

			if(str.getBytes().length > 140){str = str.substring( 0, 70);}
			BookMark[mode] = str;
			tBookMark[mode] = trimstr(BookMark[mode], width - 29);
			//if((stat2 & 0x0010000) != 0){	//�u�b�N�}�[�N���\������Ă���
			//	showBookMark();
			//}
			BookMarkData[mode * 3] = b;	//board
			BookMarkData[mode * 3 + 1] = t;	//thread
			BookMarkData[mode * 3 + 2] = r;	//res
			///System.out.println(BookMark[mode] + " " + b + " " + t + " " + r);
			if(mode >= data[66]){
				data[66] = mode + 1;
			}
			SaveBookMark(mode);
			stat |= 0x1000000;	//��ʍX�V
			return mode;
		}
		return -1;
	}
	/**
	 * �������v�鏈��
	 * @param val �Ώ�
	 * @return int�l�ł̌���
	 */
	public final int GetDigits(int val){	//������Ԃ�
		//�e�ʏd��
		return Integer.toString(val).length();
	}
	/**
	 * �L���b�V���`�F�b�N����
	 * @see MainCanvas#makeTL
	 * @see MainCanvas#makeRes
	 */
	public final synchronized void chkcache(){
		int i;

		if((stat & 0x40000) != 0){//res
			if(data[6] == nCacheTo[nCacheIndex] - nCacheSt[nCacheIndex]){	//���ǂ�ł���L���b�V���̒��ōŌ�
				stat3 &= ~0x0808000;
				httpinit(12,nCacheBrd[nCacheIndex],nCacheTo[nCacheIndex] + 1,nCacheTo[nCacheIndex] + 1,nCacheTh[nCacheIndex][0]);
				if((stat3 & 0x0008000) != 0){	//�L���b�V�������݂���
					stat3 |= 0x0800000;	//��>>��~>>�ɂ���
				}
			}
			if(data[6] == 0 && nCacheSt[nCacheIndex] > 1){	//���ǂ�ł���L���b�V���̒��ōŏ�
				//stat3 &= ~0x0808000;
				stat3 &= ~0x0408000;
				httpinit(12,nCacheBrd[nCacheIndex],nCacheSt[nCacheIndex] - 1,nCacheSt[nCacheIndex] - 1,nCacheTh[nCacheIndex][0]);
				if((stat3 & 0x0008000) != 0){	//�L���b�V�������݂���
					//stat3 |= 0x0800000;	//��>>��~>>�ɂ���
					stat3 |= 0x0400000;	//<<�ڂ�<<�~�ɂ���
				}
			}
			//���łɃ����N�悪�L���b�V������Ă��邩�`�F�b�N����
			if((data[57] & 0x00004000) != 0){
				if(nCacheInfo == null || nCacheInfo.length < data[59]){
					nCacheInfo = new int[data[59]];
				}
				for(i = 0; i < data[59]; i++){
					nCacheInfo[i] = 0;
					stat3 &= ~0x0008000;
					Link(null, i + 1);
					if((stat3 & 0x0008000) != 0){	//�L���b�V�������݂���
						nCacheInfo[i] = 1;
					}
				}
			}
		} else if( (stat & 0x10000) != 0 ){	//threadlist
//			if(data[4] == nCacheTo[nCacheIndex] - nCacheSt[nCacheIndex]){	//���ǂ�ł���L���b�V���̒��ōŌ�
				stat3 &= ~0x0808000;
				httpinit(11,nCacheBrd[nCacheIndex],nCacheTo[nCacheIndex] + 1,nCacheTo[nCacheIndex] + 1,0);
				if((stat3 & 0x0008000) != 0){	//�L���b�V�������݂���
					stat3 |= 0x0800000;	//��>>��~>>�ɂ���
				}
//			}
//			if(data[4] == 0 && nCacheSt[nCacheIndex] > 1){	//���ǂ�ł���L���b�V���̒��ōŏ�
				stat3 &= ~0x0408000;
				httpinit(11,nCacheBrd[nCacheIndex],nCacheSt[nCacheIndex] - 1,nCacheSt[nCacheIndex] - 1,0);
				if((stat3 & 0x0008000) != 0){	//�L���b�V�������݂���
					stat3 |= 0x0400000;	//<<�ڂ�<<�~�ɂ���
				}
//			}
			//���łɃ����N�悪�L���b�V������Ă��邩�`�F�b�N����
			if((data[57] & 0x00004000) != 0){
				if(nCacheInfo == null || nCacheInfo.length < data[59]){
					nCacheInfo = new int[data[59]];
				}
				for(i = 0; i < data[59]; i++){
					nCacheInfo[i] = 0;
					stat3 &= ~0x0008000;
					httpinit(12,nCacheBrd[nCacheIndex],1,1,nCacheTh[nCacheIndex][i]);
					if((stat3 & 0x0008000) != 0){	//�L���b�V�������݂���
						nCacheInfo[i] = 1;
					}
				}
			}
		}
	}
	/**
	 * �X���b�h���X�g�̍쐬�B�󂯎�����f�[�^��ϊ����鏈���B�`�掩�̂�paint()�ōs���B
	 * @see MainCanvas#paint
	 */
	public final void makeTL(){ //make thread list
		int i, j, k;
		byte[] b;
		if((stat2 & 0x40000000) != 0){	//makeTL�ŏ��������s��
			strdata[9] = CacheTitle[nCacheIndex];


			//�e�ʏd��
			//i_length = CacheTitle.length;
			for(j = 0;j < /*i_length*/CacheTitle.length;j++){
				if(nCacheTTL[j] >= 0){
					nCacheTTL[j]++;
					//�����͈͂̃L���b�V���͖����ɂ���
					if((stat & 0x0008000) != 0 && j != nCacheIndex && CacheResData[j] == null && nCacheBrd[j] == nCacheBrd[nCacheIndex] && nCacheSt[j] >= nCacheSt[nCacheIndex] && nCacheTo[j] <= nCacheTo[nCacheIndex]){//�L���b�V���q�b�g
						nCacheTTL[j] = -1;
					}
				}
			}
			nCacheTTL[nCacheIndex] = 0;	//�L���b�V����L���ɂ���
			data[77] = ((Linklist[data[60]] / 1000) % 10000) * (data[30] + data[34]);	//�c�X�N���[���I�t�Z�b�g
			if((stat & 0x0004000) != 0){	//�I�𒆂̏ꍇ
				data[24] = data[10];data[25] = data[11];	//�ꏊ�̕ۑ�
				stat &= ~0x0004000;	//�I�𒆉���
				removeCommand(command[7]);//����
				removeCommand(command[9]);//�o�^
			} else if((stat3 & 0x0000040) != 0){	//�߂�Ƃ��̏ꍇ
				data[77] = Linkrefsc[data[64]];	//�X�N���[����
				data[64]--;
				stat3 ^= 0x0000040;
			} else if((stat2 & 0x0010000) != 0){//�u�b�N�}�[�N
				stat3 |= 0x0000020;	//�u�b�N�}�[�N����ɃW�����v�������Ƃ������t���O
				stat2 ^= 0x0010000;	//�u�b�N�}�[�N����
				removeCommand(command[3]);
				removeCommand(command[0]);
				//removeCommand(command[1]);
			}
			stat &= ~0x0040000;	//���X�\�����̂Ƃ��͉���
			stat |= 0x0010000;	//�X���I��
			ListBox = StrList[2];//	data[12] = height - 13 - (data[30] + 3) * data[56];	data[13] = 13 + (data[30] + 3) * data[56];
		}
		//������
		outarray = new ByteArrayOutputStream(128);
		if(DivStr == null || DivStr.length < 100){
			DivStr = new String[100];
		}
		data[85] = 0;	//DivStr�̎g�p��(DivStr�ŏ������Ă���s)
		data[59] = 0;	//Linklist�̎g�p��
		//i_length = CacheBrdData[nCacheIndex].length;
		int b_length;
		//�X���b�h���X�g�̕`��i���ۂ͕`��p�ϐ�outarray�ւ̑���B���outarray�̒��g��print�ɂĕ`��j
		for(j = 0; j < /*i_length*/CacheBrdData[nCacheIndex].length; j++){
			b = Integer.toString(nCacheSt[nCacheIndex] + j).getBytes();
			//�X���̕��єԍ�
			outarray.write(b, 0, b.length);
			outarray.write(0x3A);	//:
			//�^�C�g��(���X��)
			b = (CacheBrdData[nCacheIndex][j] + "(" + Integer.toString(nCacheBrdData[nCacheIndex][j]) + ")").getBytes();
			int w = GetDigits(nCacheSt[nCacheIndex] + j) + 1;
			data[61] = w;	//Link�o�C�g��
			data[62] = 0;			//Link�̃X�^�[�g��
			data[63] = data[85];	//Link�̃X�^�[�g�s
			addLinklist();
			b_length = b.length;
			for(i = 0;i < b_length;i++){
				//	0x81					0x9F	0xE0					0xFE
				//if((-127 <= b[i] && b[i] <= -97) || (-32 <= b[i] && b[i] <= -2)){//SJIS�̂P�o�C�g��
				if(0 > b[i] && (b[i] <= -97 || -32 <= b[i])){//SJIS�̂P�o�C�g��(�������[�h(����~�X���N���邩��))
					w += 2;
					if(data[92] != 1 && w > data[42]){//���߂�ꂽ�������z���Ă��܂�����
						addDivStr2();
						w = 2;
						if(data[92] == 2){
							for(k = 0; k < data[61]; k++){
								outarray.write(' ');
							}
							w += data[61];
						}
					}

					outarray.write(b[i]);	i++;
					outarray.write(b[i]);
				} else {//1byte
					w++;
					if(data[92] != 1 && w > data[42]){//���߂�ꂽ�������z���Ă��܂�����
						addDivStr2();
						w = 1;
						if(data[92] == 2){
							for(k = 0; k < data[61]; k++){
								outarray.write(' ');
							}
							w += data[61];
						}
					}
					outarray.write(b[i]);
				}
			}
			if(w != 0){
				addDivStr2();
			}
		}
//		data[60] = data[4];
//		data[77] = ((Linklist[data[60]] / 1000) % 10000) * (data[30] + data[34]);	//�c�X�N���[���I�t�Z�b�g
		if((data[57] & 0x00008000) == 0){	//�X�����Ƃ̃X�N���[�������Ă��Ȃ��ꍇ
			i = (data[85] + 5/*2*/) * (data[30] + data[34]) - height;
			if(data[77] > i){data[77] = i;}
		}
		if(data[77] < 0){data[77] = 0;}
		data[48] = 0;	//���X�N���[���I�t�Z�b�g
//		if(data[59] != 0){		//Linklist�̎g�p��
//			data[60] = 0;		//Linkfocus
//		} else {
//			data[60] = -1;		//Linkfocus
//		}
		stat &= ~0x0000100;	//���X�g�{�b�N�X����
		//stat |= 0x1000000;	//��ʍX�V
		//addCommand(command[9]);//�o�^
		addCommand(command[0]);//�ƭ�
		addCommand(command[6]);//�߂�
		data[10] = 0;data[11] = 0;
		chkcache();	//�L���b�V���`�F�b�N
	}
	//public final void FastMaRE(/*int option*/){	//FastMakeResElements
	/**
	 * ���X�̍쐬�B�󂯎�����f�[�^��ϊ����鏈���B�`�掩�̂�print()�ōs���B
	 */
	public final void makeRes(){
		int i, j, k, n;
		data[77] = -data[30];	//�c�X�N���[���I�t�Z�b�g
		data[48] = 0;			//���X�N���[���I�t�Z�b�g
		if((stat2 & 0x80000000) != 0){	//makeRes�ŏ��������s��
			strdata[9] = CacheTitle[nCacheIndex];
			i_length = CacheTitle.length;
			for(j = 0;j < i_length;j++){
				if(nCacheTTL[j] >= 0){
					nCacheTTL[j]++;
					if(CacheBrdData[j] == null && nCacheTh[j] != null && nCacheTh[j][0] == nCacheTh[nCacheIndex][0] && nCacheBrd[j] == nCacheBrd[nCacheIndex]){
						nCacheAll[j] = nCacheAll[nCacheIndex];	//All�����ŐV�̂��̂ɍX�V
					}
				}
			}
			nCacheTTL[nCacheIndex] = 0;	//�L���b�V����L���ɂ���
			//����������@�\
			j = data[67] + data[68];
			if((data[57] & 0x00000200) != 0 && nCacheBrd[nCacheIndex]/*data[3]*/ == BookMarkData[j * 3] && nCacheTh[nCacheIndex][0]/*data[2]*/ == BookMarkData[j * 3 + 1]){
				if(BookMarkData[j * 3 + 2] >= 0 && BookMarkData[j * 3 + 2] <= nCacheTo[nCacheIndex]/*data[8]*/){
					BookMarkData[j * 3 + 2] = nCacheTo[nCacheIndex]/*data[8]*/ + 1;
					SaveBookMark(j);
				}
			}
			if((stat3 & 0x0000040) != 0){	//�߂�Ƃ��̏ꍇ
				data[77] = Linkrefsc[data[64]];	//�X�N���[����
				data[64]--;
				stat3 ^= 0x0000040;
			} else if((stat2 & 0x0010000) != 0){//�u�b�N�}�[�N
				stat2 |= 0x0020000;	//�u�b�N�}�[�N����X���b�h�ɃW�����v�������Ƃ������t���O
				stat2 ^= 0x0010000;	//�u�b�N�}�[�N����
				removeCommand(command[3]);
				removeCommand(command[0]);
			}
			stat &= ~0x0010000;	//�ڑI�𒆂̂Ƃ��͉���
			stat |= 0x0040000;	//���X�\����
			ListBox = StrList[3];
		}
		stat2 &= ~0x80000000;
		//alist = new ArrayList();
		outarray = new ByteArrayOutputStream(128);
		//String str;
		//ResElements = new String[1];
		while(CacheResData[nCacheIndex][data[6]] == null/*data[45] < data[6] + 1*/){
//			try{Thread.sleep(100);}catch (Exception e){}
			wait(100);
		}
		byte b[] = CacheResData[nCacheIndex][data[6]]/*Res[data[6]]*//*.getBytes()*/, b2[], b3[];
//#ifdef DEBUG
//		System.out.println(new String(b));
//#endif
		//÷���ޯ���p
		//resdata = "";
		//resdata = resdata + new String(b);
		resdata = b;

		//if(resdata.indexOf(ngword) != -1){

		/*if(ngword.indexOf('|') != -1){
			int ngi = 0;
			int abon = 0;
			while(ngword.indexOf('|',ngi) != - 1 || abon != 1) {
				ngi = ngword.indexOf('|',ngi);
				String ngstr = ngword.substring(ngword.indexOf('|',ngi + 1));
				if(resdata.indexOf(ngstr) != -1){
					abon = 1;
				}
			}
			if(abon == 1){
				String newstr = resdata.substring(resdata.lastIndexOf('\r'));
				newstr = newstr.substring(newstr.indexOf('\t'));
				newstr = "���ځ[��\n" + newstr;
			//System.out.println(newstr);
				b =  newstr.getBytes();
			}
		}*/
		//	b = "���ځ[��\n���ځ[��\t���ځ[��\t���ځ[��".getBytes();
		//}
		//������
		//if(DivStr == null){
		//	DivStr = new String[100];
		//}
		if(iDivStr == null || iDivStr.length < 100){
			iDivStr = new int[100];
		}
		data[86] = 0;	//���݂̍s�̕�����(iDivStr�Ŏg�p����ϐ�)
		data[85] = 0;	//DivStr�̎g�p��(DivStr�ŏ������Ă���s)
		data[59] = 0;	//Linklist�̎g�p��
		//if(Linklist == null){
		//	Linklist = new int[20];
		//}
		//�{��
		i = SubMaRE(b, 0, 0);
		//��؂��
		n = GetDigits(nCacheAll[nCacheIndex]/*data[9]*/);
		k = (width - (n + 10/*4 + 2*/) * data[33]) / data[30];
		//str = "�|";
		b2 = /*str*/"�\".getBytes();


		for(j = 0;j < k;j++){
			outarray.write(b2, 0, b2.length);
		}
		//str = "All:" + data[9];
		b3 = ("AllRes:" + nCacheAll[nCacheIndex]/*data[9]*/).getBytes();
		outarray.write(b3, 0, b3.length);
		j += b3.length;
		outarray.write(b2, 0, b2.length);
		j++;
		if(k * data[30] + (n + 7/*4 + 2 + 1*/) * data[33] < width){
			outarray.write(45);
			j++;
		}
		data[86] += j;
		addDivStr();
		//���O�@���[���@���ԁ@�h�c
		//EZPLUS
		//resstr = resstr + "\r���O:"/*StrList[10][15]*/ + ResElements[0] + "\r\uE521 " + ResElements[1] + "\r\uE56A "/*StrList[10][16]*/ + ResElements[2];
		//
		//resstr = resstr + "\r\uE301 "/*StrList[10][15]*/ + ResElements[0] + "\r\uE101 " + ResElements[1] + "\r\uE024 "/*StrList[10][16]*/ + ResElements[2];
		//DOJA
		//resstr = resstr + "\r\ue6b1 "/*StrList[10][15]*/ + ResElements[0] + "\r\ue6d3 " + ResElements[1] + "\r\ue6ba "/*StrList[10][16]*/ + ResElements[2];
		j = i;

		//str = "\ue6b1 ";
		b2 = Emoji[0].getBytes();//str.getBytes();
		outarray.write(b2, 0, b2.length);
		data[86] += 2;
		i = SubMaRE(b, i, 3);

		//outarray.write(b, j, i - j - 1);
		//ResElements[0] = outarray.toString();	//���O
		strdata[3] = new String(b, j, i - j - 1);	//���O
		//outarray.reset();
		//str = "\ue6d3 ";
		/*for(int i_a=0;i_a >= b_length;i_a++){
			name_a[i_a] = strdata[3].substring(i_a, data[42]);
			System.out.print(name_a[i_a]);
			i_a = data[42];
		}*/


		b2 = Emoji[1].getBytes();//str.getBytes();
		outarray.write(b2, 0, b2.length);
		data[86] += 2;
		i = SubMaRE(b, i, 3);
		//str = "\ue6ba ";
		b2 = Emoji[2].getBytes();//str.getBytes();
		outarray.write(b2, 0, b2.length);
		data[86] += 2;
		i = SubMaRE(b, i, 3);




		if(i < b.length){	//ID����
			b2 = Emoji[3].getBytes();
			outarray.write(b2, 0, b2.length);
			data[86] += 3;
			i = SubMaRE(b, i, 3);


/*
*/
//		} else if(ResElements.length >= 4){
//			//resstr = ResElements[3] + resstr;
		}
		//DivStr = SeparateString(resstr, -1);
//		if( (stat2 & 0x0000080) != 0 ){	//AA MODE
//			DivStr = FastAASeparateByte(resstr.getBytes(), '\r');
//		} else {
//			DivStr = FastSeparateByte(resstr.getBytes());
//		}
		//DivStr = new String[vec.size()];
		//vec.copyInto(DivStr);
		//DivStr = alist.toArray();
		//data[85]--;
		resstr = outarray.toString();



		if((stat2 & 0x0000002) == 0){	//�\���ݒ�ł͂Ȃ��Ƃ�
			stat &= ~0x0000100;	//���X�g�{�b�N�X����
		}
		addCommand(command[0]);//���j���[
		addCommand(command[6]);//�߂�
		if(data[59] != 0){		//Linklist�̎g�p��
			data[60] = 0;		//Linkfocus
			if(data[77] != -data[30]){
				setLink();
			}
		} else {
			data[60] = -1;		//Linkfocus
		}
		//data[65] = 0;	//Link�̑I���X�L�b�v��
		chkcache();	//�L���b�V���`�F�b�N

		stat |= 0x1000000;	//��ʍX�V

	}
	/**
	 * makeRes�ōs��Link���ʁA���s���ʓ��X�̉��x���s�����������\�b�h�Ƃ��ĕ������Ă������
	 * @param b data
	 * @param i offset
	 * @param w �s��
	 * @return i - offset(�ڍוs��)
	 */
	public final int SubMaRE(byte b[], int i, int w){	//b:data i:offset
//		if((stat2 & 0x0000080) == 0){	//!AA MODE
		i_length = b.length;
		for(;i < i_length;i++){
			if((stat3 & 0x0000003) != 0){	//LinkON
				if((stat3 & 0x0000001) != 0){	//LinkON(�����X���b�h��)
					if((b[i] < 48 || b[i] > 57) && b[i] != 45){	//�����N�ɋ�����Ă�����̂ł͂Ȃ��ꍇ
						if(/*data[61] > 2 && */b[i-1] != 62 && b[i-1] != 132){
							Linkurllist[data[59]] = new String(b, i-data[61], data[61]);
							addLinklist();
						}
						stat3 ^= 0x0000001;
					} else {
						data[61]++;
					}
//				} else if((stat3 & 0x0000002) != 0){	//LinkON(URL)
				} else {	//LinkON(URL)
					if((b[i] > 42 && b[i] < 127) || (b[i] > 34 && b[i] < 39)){	//�����N�ɋ�����Ă�����̂̏ꍇ
						data[61]++;
					} else {
						if(data[61] > 10){
							//ttp�ł̃����N�̍ۂɋN����s���h�~
							byte b2 = b[i-data[61]];
							b[i-data[61]] = 104;	//h
							Linkurllist[data[59]] = new String(b, i-data[61], data[61]);
							b[i-data[61]] = b2;
							addLinklist();
						}
						stat3 ^= 0x0000002;
					}
				}
			}
			//	0x81					0x9F	0xE0					0xFE
			//if((-127 <= b[i] && b[i] <= -97) || (-32 <= b[i] && b[i] <= -2)){//SJIS�̂P�o�C�g��
			if(0 > b[i] && (b[i] <= -97 || -32 <= b[i])){//SJIS�̂P�o�C�g��(�������[�h(����~�X���N���邩��))
				w += 2;
				if(w > data[42] && (stat2 & 0x0000080) == 0){//���߂�ꂽ�������z���Ă��܂�����
					addDivStr();
					w = 2;
				}

				outarray.write(b[i]);	i++;
				outarray.write(b[i]);
			} else if(b[i] == '\r'/* || b[i] == '\n'*/){
				addDivStr();	w = 0;
				continue;
			} else if(b[i] == '\t'){
				i++;
				break;
			} else {//1byte
				w++;
				//if(i+1 < b.length && (b[i] == 62 || (b[i] == 129 && b[i+1] == 132))){// >
				if(b[i] == 62 && i+1 < b.length){// >xxx or >>xxx
					if(w+1 > data[42] && (stat2 & 0x0000080) == 0){//���߂�ꂽ�������z���Ă��܂�����
						addDivStr();
						w = 1;
					}

					//if(w != 0){addDivStr();	w = 0;}
					stat3 |= 0x0000001;	data[63] = data[85];
					Linklist2[data[59]] = data[86];
					data[62] = w-1;
					//if(b[i+1] == 62 || b[i+1] == 132){
					if(b[i+1] == 62){	// >>xxx
						data[61] = 2;
						outarray.write(b[i]);	i++;	w++;
						data[86]++;
					} else {
						data[61] = 1;
					}
//�ȉ��̓��A�e�ʂ̏��������𗘗p
				} else if(i+10 < b.length && b[i+1] == 116 && ((b[i] == 104 && b[i+2] == 116 && b[i+3] == 112) || (b[i] == 116 && b[i+2] == 112))){	//http or ttp
					if(w+3 > data[42] && (stat2 & 0x0000080) == 0){//���߂�ꂽ�������z���Ă��܂�����
						addDivStr();
						w = 1;
					}
					//if(w != 0){addDivStr();	w = 0;}
					stat3 |= 0x0000002;	data[61] = 4;	data[63] = data[85];
					Linklist2[data[59]] = data[86];
					data[62] = w-1;	//Link�̃X�^�[�g��
					//if(b[i] == 116){	//ttp
					//	outarray.write(104);	w++;	//h
					//} else {
					//	outarray.write(b[i]);	i++;	w++;
					//}
					//w += 2;
					if(b[i] == 104){	//http
						i++;
					}
					outarray.write(104);	//h

					w += 3;
					outarray.write(b[i]);	i++;//	w++;	//t
					outarray.write(b[i]);	i++;//	w++;	//t
					data[86] += 3;
/*
				} else if(b[i] == 104 && i+10 < b.length && b[i+1] == 116 && b[i+2] == 116 && b[i+3] == 112){	//http
					//if(w != 0){addDivStr();	w = 0;}
					stat3 |= 0x0000002;	data[62] = w;	data[61] = 4;	data[63] = data[85];
					outarray.write(b[i]);	i++;	w++;
					outarray.write(b[i]);	i++;	w++;
					outarray.write(b[i]);	i++;	w++;
				} else if(b[i] == 116 && i+10 < b.length && b[i+1] == 116 && b[i+2] == 112){	//ttp
					//if(w != 0){addDivStr();	w = 0;}
					stat3 |= 0x0000002;	data[62] = w;	data[61] = 4;	data[63] = data[85];
					outarray.write(104);
					outarray.write(b[i]);	i++;	w++;
					outarray.write(b[i]);	i++;	w++;
*/				} else {
					if(w > data[42] && (stat2 & 0x0000080) == 0){//���߂�ꂽ�������z���Ă��܂�����
						addDivStr();
						w = 1;
					}
				}
				outarray.write(b[i]);
			}
			data[86]++;
		}
//		} else {
//			for(;i < b.length;i++){
//				if(b[i] == '\r'/* || b[i] == '\n'*/){
//					//alist.addElement(outarray.toString());
//					addDivStr();
//					w = 0;
//				} else if(b[i] == '\t'){
//					i++;
//					break;
//				} else {//1byte
//					outarray.write(b[i]);
//					w++;
//				}
//			}
//		}

		if(w != 0){
			//alist.addElement(outarray.toString());
			addDivStr();
			//w = 0;
		}
		//outarray.reset();
//		System.out.println("i:" + i);
		return i;
	}

	public final void addDivStr2(){
		DivStr[data[85]] = outarray.toString();	data[85]++;
		outarray.reset();
		if(data[85] == DivStr.length){
			String[] tmp = new String[DivStr.length*2];
			//int i;
			//for(i = 0;i < DivStr.length;i++)
			//	tmp[i] = DivStr[i];
			System.arraycopy(DivStr, 0, tmp, 0, data[85]);
			DivStr = tmp;
		}
	}

	public final void addDivStr(){
		if(data[85] == 0){
			iDivStr[data[85]] = data[86];/*outarray.size()*/
		} else {
			iDivStr[data[85]] = iDivStr[data[85]-1] + data[86];/*outarray.size()*/
		}
		data[86] = 0;
		data[85]++;
		if(data[85] == iDivStr.length){
			int[] tmp = new int[iDivStr.length*2];
			//int i;
			//for(i = 0;i < DivStr.length;i++)
			//	tmp[i] = DivStr[i];
            System.arraycopy(iDivStr, 0, tmp, 0, data[85]);
			iDivStr = tmp;
		}


	}

	/**
	 * �����N�p�z��Ƀ����N��ǉ��B�����N�̎w��͑S�ăO���[�o���ϐ����g�p�B
	 */
	public final void addLinklist(){
		if(data[61] > 213){data[61] = 213;}
		Linklist[data[59]] = (data[61] * 10000 + data[63]) * 1000 + data[62];
		//Linkurllist[data[59]] = null;
		data[59]++;
		if(data[59] == Linklist.length){
			int[] tmp = new int[Linklist.length*2];
			//int i;
			//for(i = 0;i < Linklist.length;i++)
			//	tmp[i] = Linklist[i];
            System.arraycopy(Linklist, 0, tmp, 0, data[59]);
			Linklist = tmp;

			tmp = new int[Linklist.length*2];
            System.arraycopy(Linklist2, 0, tmp, 0, data[59]);
			Linklist2 = tmp;
			String[] tmp2 = new String[Linklist.length*2];
            System.arraycopy(Linkurllist, 0, tmp2, 0, data[59]);
			Linkurllist = tmp2;
		}
	}

	/**
	 * �\�����Ă���̈�Ƀ����N�����邩�ǂ����𔻒�H
	 */
	public final void setLink(){
		int i, j, k;
		k = data[77] / (data[30] + data[34]);//	if(0 > i){i = 0;}	//DivStr�̏����n��
		for(j = 0;j < data[59];j++){
			i = (Linklist[j] / 1000) % 10000;
			if(i > k/*i >= k*/){	//�����N����Ƃ���͕\�����ɂ���ꍇ
				data[60] = j;
				break;
			}
		}
/*
		k = data[77] / (data[30] + data[34]);//	if(0 > i){i = 0;}	//DivStr�̏����n��
		for(j = 0;j < data[59];j++){
			i = (Linklist[j] / 1000) % 10000;
			if(i > k){	//�����N����Ƃ���͕\�����ɂ���ꍇ
				data[60] = j;
				break;
			}
		}
*/
	}

	/**
	 * �{�b�N�X�̌`�ł̕�����\������
	 * @param g Graphics
	 * @param x x���W
	 * @param y y���W
	 * @param str �����s�ɓn��ꍇ�B��������g�p�������ꍇ��str2=null�̕K�v������B
	 * @param str2 1�s�݂̂̏ꍇ�B
	 * @param yspace �㉺��
	 * @return ret
	 */
//str:�����s�ɂ킽��ꍇ�@str2:�P�s�݂̂̏ꍇ str���g�p�������ꍇ�́Astr2=null�̕K�v������܂��B
	public final int DrawBoxStr(Graphics g , int x , int y , String[] str , String str2 , int yspace/*�㉺��*/){
		int i = 16 * data[33], ret = 0;
		if(str2 != null){
			str = new String[1];	str[0] = str2;
			i = str2.getBytes().length * data[33];
			ret = x + i / 2;//- i / 2 + i;
		}
		int len = str.length;

		if(i % 2 == 1){i++;}
		x -= i / 2;
		y -= data[30]*len/2;

		g.setColor(ColScm[6],ColScm[7],ColScm[8]);	//�����F 192,192,192
		g.fillRect( x + 1, y - 1 - yspace, i + 3, data[30]*len + 4 + yspace * 2);
		g.setColor(ColScm[0],ColScm[1],ColScm[2]);//2ch�̃X���w�i�F 239,239,239
		g.fillRect( x - 1, y - 2 - yspace, i + 2, data[30]*len + 2 + yspace * 2);

		g.setColor(ColScm[9],ColScm[10],ColScm[11]);	//���Ԃ̔Z�� 128,128,128
		g.drawRect( x - 2, y - 3 - yspace, i + 3, data[30]*len + 3 + yspace * 2);
		//�p���ۂ߂�
		//g.drawLine( x - 1, y - 3 - yspace, x + i, y - 3 - yspace);//��
		//g.drawLine( x - 1, y + data[30]*len + yspace, x + i, y + data[30]*len + yspace);//��
		//g.drawLine( x - 2, y - 2 - yspace, x - 2, y + data[30]*len + yspace - 1);//��
		//g.drawLine( x + i + 1, y - 2 - yspace, x + i + 1, y + data[30]*len + yspace);//�E

		g.setColor(ColScm[3],ColScm[4],ColScm[5]);	//�����̐F 0,0,0
		for(i = 0;i < len;i++){
			g.drawString(str[i], x, y + data[30] * i - 1, 20/*g.TOP|g.LEFT*/);
		}
		return ret;
	}

	/**
	 * �t�H���g�̎w��B���݁A�S�Ă̋@��ő������Ȃǂ͈Ӗ����Ȃ��Ă��Ȃ��B���������������B
	 */
	public final void SetFont(){
		int i;
		if(data[36] == 0){	//SMALL
			//font = Font.getFont(Font.FACE_MONOSPACE,Font.STYLE_BOLD,Font.SIZE_SMALL);
			if(data[98] == 0){
				if(data[99] == 0){ //PLAIN
					font = font.getFont(font.FACE_SYSTEM,font.STYLE_PLAIN,font.SIZE_SMALL);
				}else if(data[99] == 1){
					font = font.getFont(font.FACE_MONOSPACE,font.STYLE_PLAIN,font.SIZE_SMALL);
				}else if(data[99] == 2){
					font = font.getFont(font.FACE_PROPORTIONAL,font.STYLE_PLAIN,font.SIZE_SMALL);
				}
			}else if(data[98] == 1){ //BOLD
				if(data[99] == 0){
					font = font.getFont(font.FACE_SYSTEM,font.STYLE_BOLD,font.SIZE_SMALL);
				}else if(data[99] == 1){
					font = font.getFont(font.FACE_MONOSPACE,font.STYLE_BOLD,font.SIZE_SMALL);
				}else if(data[99] == 2){
					font = font.getFont(font.FACE_PROPORTIONAL,font.STYLE_BOLD,font.SIZE_SMALL);
				}
			}else if(data[98] == 2){
				if(data[99] == 0){ //ITALIC
					font = font.getFont(font.FACE_SYSTEM,font.STYLE_ITALIC,font.SIZE_SMALL);
				}else if(data[99] == 1){
					font = font.getFont(font.FACE_MONOSPACE,font.STYLE_ITALIC,font.SIZE_SMALL);
				}else if(data[99] == 2){
					font = font.getFont(font.FACE_PROPORTIONAL,font.STYLE_ITALIC,font.SIZE_SMALL);
				}
			}

		} else if(data[36] == 2) {	//LARGE
			//font = Font.getFont(Font.FACE_MONOSPACE,Font.STYLE_BOLD,Font.SIZE_LARGE);
			if(data[98] == 0){
				if(data[99] == 0){
					font = font.getFont(font.FACE_SYSTEM,font.STYLE_PLAIN,font.SIZE_LARGE);
				}else if(data[99] == 1){
					font = font.getFont(font.FACE_MONOSPACE,font.STYLE_PLAIN,font.SIZE_LARGE);
				}else if(data[99] == 2){
					font = font.getFont(font.FACE_PROPORTIONAL,font.STYLE_PLAIN,font.SIZE_LARGE);
				}
			}else if(data[98] == 1){
				if(data[99] == 0){
					font = font.getFont(font.FACE_SYSTEM,font.STYLE_BOLD,font.SIZE_LARGE);
				}else if(data[99] == 1){
					font = font.getFont(font.FACE_MONOSPACE,font.STYLE_BOLD,font.SIZE_LARGE);
				}else if(data[99] == 2){
					font = font.getFont(font.FACE_PROPORTIONAL,font.STYLE_BOLD,font.SIZE_LARGE);
				}
			}else if(data[98] == 2){
				if(data[99] == 0){
					font = font.getFont(font.FACE_SYSTEM,font.STYLE_ITALIC,font.SIZE_LARGE);
				}else if(data[99] == 1){
					font = font.getFont(font.FACE_MONOSPACE,font.STYLE_ITALIC,font.SIZE_LARGE);
				}else if(data[99] == 2){
					font = font.getFont(font.FACE_PROPORTIONAL,font.STYLE_ITALIC,font.SIZE_LARGE);
				}
			}
		} else {//if(data[36] == 1){	//MIDIUM
			//font = Font.getFont(Font.FACE_MONOSPACE,Font.STYLE_BOLD,Font.SIZE_MEDIUM);
			if(data[98] == 0){
				if(data[99] == 0){
					font = font.getFont(font.FACE_SYSTEM,font.STYLE_PLAIN,font.SIZE_MEDIUM);
				}else if(data[99] == 1){
					font = font.getFont(font.FACE_MONOSPACE,font.STYLE_PLAIN,font.SIZE_MEDIUM);
				}else if(data[99] == 2){
					font = font.getFont(font.FACE_PROPORTIONAL,font.STYLE_PLAIN,font.SIZE_MEDIUM);
				}
			}else if(data[98] == 1){
				if(data[99] == 0){
					font = font.getFont(font.FACE_SYSTEM,font.STYLE_BOLD,font.SIZE_MEDIUM);
				}else if(data[99] == 1){
					font = font.getFont(font.FACE_MONOSPACE,font.STYLE_BOLD,font.SIZE_MEDIUM);
				}else if(data[99] == 2){
					font = font.getFont(font.FACE_PROPORTIONAL,font.STYLE_BOLD,font.SIZE_MEDIUM);
				}
			}else if(data[98] == 2){
				if(data[99] == 0){
					font = font.getFont(font.FACE_SYSTEM,font.STYLE_ITALIC,font.SIZE_MEDIUM);
				}else if(data[99] == 1){
					font = font.getFont(font.FACE_MONOSPACE,font.STYLE_ITALIC,font.SIZE_MEDIUM);
				}else if(data[99] == 2){
					font = font.getFont(font.FACE_PROPORTIONAL,font.STYLE_ITALIC,font.SIZE_MEDIUM);
				}
			}
		}
		data[30] = font.getHeight();
		if(data[30] % 2 == 1){
			data[30]++;
		}

		data[31] = font.getBaselinePosition();
		data[32] = font.getHeight() - font.getBaselinePosition();
		data[33] = font.charWidth('M');

		data[12] = data[30] + 3;			//LIST Y���W
		data[13] = height - (data[30] + 3);	//LIST �c��
		data[42] = width / data[33];	//��s�̕���(byte)��
		//�������̂��߂Ƀg���~���O�ς݂̃u�b�N�}�[�N�������炩���ߍ쐬
		for(i = 0;i < data[66];i++){
			tBookMark[i] = trimstr(BookMark[i], width - 29);
		}
	}

	/**
	 * ������̎O�p�`��`��
	 * @param g Graphics
	 * @param x x���W
	 * @param y y���W
	 * @param direction 0:�� 1:��
	 */
	public final void DrawTriangle(Graphics g, int x, int y, int direction /*0:�� 1:��*/){
		int i;
		for (i = 2; i >= 0; i--) {
			if(direction == 0){
				g.drawLine(x - i, y + 2 - i, x + i, y + 2 - i);
			} else {
				g.drawLine(x - i, y + 2 + i, x + i, y + 2 + i);
			}
		}
	}

	/**
	 * �ʐM�����̓�����B��������thttpget()�ɔ��ł����httpgetdata()�ɔ��
	 * @param mode 0:�ꗗ�̓Ǎ� 1:�X���b�h���X�g 2:���X (3:Menu) 4:�X������ 5:URL���ڎw�� 6:��URL���擾 7:�������ݏ��� 11:�X���b�h���X�g(�L���b�V���`�F�b�N) 12:���X(�L���b�V���`�F�b�N)
	 * @param brd board
	 * @param st start(1�`)
	 * @param to end
	 * @param th thread
	 * @see MainCanvas#thttpget()
	 * @see MainCanvas#httpgetdata()
	 */
	public final void httpinit(int mode/*0:�ꗗ�̓Ǎ� 1:�X���b�h���X�g 2:���X (3:Menu) 4:�X������ 5:URL���ڎw�� 6:��URL���擾 7:�������ݏ��� 11:�X���b�h���X�g(�L���b�V���`�F�b�N) 12:���X(�L���b�V���`�F�b�N)*/,
	 					int brd/*board*/, int st/*start(1�`)*/, int to/*end*/, int th/*thread*/){
						//�ŐV���X�擾����st=-1,to=�擾���郌�X�̐�
		int i, j;
		int cachestat = 0;
		int savelinkref = 0;
		//data[72] = 0;
		if(mode < 10){

			stat3 &= ~0x0000200;	//�Q�ƌ���ۑ����Ă���t���O��������
			//�߂�Ƃ��łȂ��A�Q�ƌ���ۑ����Ȃ��t���O�������Ă��Ȃ��A�ēǍ��łȂ��Ƃ��ɎQ�ƌ���ۑ�
			if(/*(mode == 1 || mode == 2 || mode == 4 || mode == 5)*/mode >= 1 && mode <= 5 && (stat & 0x0008000) == 0 && (stat3 & 0x0000140) == 0/*(stat3 & 0x0000040) == 0 && (stat3 & 0x0000100) == 0*/ ){
				//addLinkref();	//�Q�ƌ��̒ǉ�
				//�b��d�l
				//�u�b�N�}�[�N�܂��͔��X�g����W�����v����Ƃ��͏�����
				if((stat2 & 0x0010000) != 0 || (stat & 0x0004000) != 0){
					data[64] = 0;
				} else {
					i = data[64]+1;
					//data[64]++;
					if( (i+1)*3 > Linkref.length){
						int[] tmp = new int[Linkref.length*2];
			            System.arraycopy(Linkref, 0, tmp, 0, Linkref.length);
						Linkref = tmp;
						tmp = new int[Linkrefsc.length*2];
			            System.arraycopy(Linkrefsc, 0, tmp, 0, Linkrefsc.length);
						Linkrefsc = tmp;
					}
					Linkref[i*3  ] = nCacheBrd[nCacheIndex];//data[3];	//b
					if((stat & 0x0010000) != 0){	//�X���I��
						Linkref[i*3+1] = 0;	//thread
						Linkref[i*3+2] = nCacheSt[nCacheIndex] + data[60];	//r
					} else {
						Linkref[i*3+1] = nCacheTh[nCacheIndex][0];//data[2]nThread[data[4]];	//thread
						Linkref[i*3+2] = nCacheSt[nCacheIndex] + data[6];//data[7];	//r
					}
					Linkrefsc[i] = data[77];	//�X�N���[����
					savelinkref = 1;
				}
			}
			stat3 &= ~0x0000100;	//�Q�ƌ���ۑ����Ȃ��t���O������
		}
		//�X���ԍ���0�̎��̓X���b�h���X�g�ǂݍ��݂ɏC������
		if(mode % 10 == 2 && th == 0){mode -= 1;}
		if((mode == 2 || mode == 12) && st > 0){
			//���݌��Ă郌�X�̒��ɃW�����v�悪����Ƃ�
			i_length = CacheTitle.length - 1;
			for(i = i_length - 1;i >= 0;i--){	//�L���b�V�����̌���
//				#ifdef DEBUG
//				System.out.println("CACHE:check" + i + "/" + CacheTitle.length + " /br" + nCacheBrd[i] + "/st" + nCacheSt[i] + "/to" + nCacheTo[i]);
//				#endif
				if(nCacheTTL[i] >= 0 && CacheBrdData[i] == null && nCacheTh[i] != null && nCacheTh[i][0] == th && nCacheBrd[i] == brd){
//					#ifdef DEBUG
//					System.out.println("CACHE:thread cachest:" + nCacheSt[i] + " cacheto:" + nCacheTo[i] + " " + stat3);
//					#endif
					if( ((stat3 & 0x0000010) == 0 && nCacheSt[i] <= st && st <= nCacheTo[i])
							|| ((stat3 & 0x0000010) != 0 && nCacheSt[i] <= to && to <= nCacheTo[i]) ){//�L���b�V���q�b�g
						if(mode == 12){
							stat3 |= 0x0008000;	//�L���b�V�������݂���
							return;
						}
						if((stat3 & 0x0000080) != 0){//��ǂ݂����邱�Ƃ������t���O
							stat3 &= ~0x0000080;	//��ǂ݂����邱�Ƃ������t���O
							return;
						}
						nCacheIndex = i;
						if((stat3 & 0x0000010) != 0){	//�I��肩��ǂ�
							data[6] = to - nCacheSt[i];
						} else {
							data[6] = st - nCacheSt[i];
						}
						stat3 &= ~0x0000010;	//���X���Ōォ��ǂރt���O�̍폜
						data[45] = nCacheTo[i] - nCacheSt[i] + 1;
						stat2 |= 0x80000000;	//makeRes�ŏ��������s��
						makeRes();
						cachestat = 1;
						break;
					} else if( (stat3 & 0x0000010) == 0 && nCacheSt[i] <= to && to <= nCacheTo[i] ){
						to = nCacheSt[i] - 1;
					} else if( (stat3 & 0x0000010) != 0 && nCacheSt[i] <= st && st <= nCacheTo[i] ){
						st = nCacheTo[i] + 1;
					}
				}
			}
			if(mode == 12){return;}
			//for(i = CacheTitle.length;i > 0;i--){
			//}
		//���������ꍇ�̃L���b�V��
		} else if((mode == 1 || mode == 11) && (stat & 0x0008000) == 0/*�ēǍ��̎��̓L���b�V�����g�p���Ȃ�*/) {
			i_length = CacheTitle.length - 1;
			for(i = i_length;i >= 0;i--){	//�L���b�V�����̌���
				if(nCacheTTL[i] >= 0 && CacheResData[i] == null && nCacheBrd[i] == brd
				 && ( ((stat3 & 0x0000010) == 0 && nCacheSt[i] <= st && st <= nCacheTo[i])
				 || ((stat3 & 0x0000010) != 0 && nCacheSt[i] <= to && to <= nCacheTo[i]) )){//�L���b�V���q�b�g
					if(mode == 11){
						stat3 |= 0x0008000;	//�L���b�V�������݂���
						return;
					}
					//data[5] = st - 1;
					//for(j = 0;j < CacheTitle.length;j++){
					//	if(nCacheTTL[j] >= 0){
					//		nCacheTTL[j]++;
					//	}
					//}
					//nCacheTTL[i] = 0;
					nCacheIndex = i;
/*
					ThreadName = CacheBrdData[i];
					data[3] = brd;
					nRes = nCacheBrdData[i];
					nThread = nCacheTh[i];
*/
					//strdata[9] = CacheTitle[i];
					if((stat3 & 0x0000010) != 0){	//�߂��Ă�Ƃ�
						//data[5] = to - 1;
						data[60] = to - nCacheSt[i];
						//data[4] = to - nCacheSt[i];
					} else {
						//data[5] = st - 1;
						data[60] = st - nCacheSt[i];
						//data[4] = st - nCacheSt[i];
					}
					stat3 &= ~0x0000010;	//���X���Ōォ��ǂރt���O�̍폜
					stat2 &= ~0x0000080;	//AAMODE����
					stat2 |= 0x40000000;	//makeTL�ŏ��������s��
					makeTL();
					stat |= 0x1000000;	//��ʍX�V
					//stat |= 0x0000100;
					cachestat = 1;
					break;
					//return;
				}
			}
			if(mode == 11){return;}
		}
		//�Q�ƌ���ۑ�����ꍇ
		if(savelinkref == 1 && (cachestat == 1 || (cachestat != 1 && (stat & 0x0010) == 0))){
			data[64]++;
			stat3 |= 0x0000200;	//�Q�ƌ���ۑ�����
		}
		if(cachestat == 1 || (stat & 0x0010) != 0){	//�ʐM��or�L���b�V����ǂޏꍇ
			return;	//�ʐM�͍s��Ȃ�
		}
		stat &= ~0x0020;//if( (stat & 0x0020) != 0 ){stat ^= 0x0020;}
		stat &= ~0x0040;//if( (stat & 0x0040) != 0 ){stat ^= 0x0040;}
		stat &= ~0x0080;//if( (stat & 0x0080) != 0 ){stat ^= 0x0080;}
		stat |= 0x0000004;	//�L�[���b�N
		strdata[5] = "v=D";	//�o�[�W���� 13
		if(mode == 6){//��URL���擾
			strdata[5] = strdata[5] + "&m=U&b=" + brd;
			//StringBuffer sbuf = new StringBuffer("v=D");
			//sbuf.append("&m=U&b=");
			//sbuf.append(brd);
		} else if(mode == 7){//��������
			String message;
			//message = bboard.getString();//.replace( ',', '\n');
			//bodytext = message;
			/*while(true){
				i = message.indexOf("\\,");	if(i < 0){break;}
				message = message.substring(0,i) + "%2C" + message.substring(i+2);
			}*/
			/*while(true){
				i = message.indexOf('%');	if(i < 0){break;}
				message = message.substring(0,i) + "%25" + message.substring(i+1);
			}
			while(true){
				i = message.indexOf('&');	if(i < 0){break;}
				message = message.substring(0,i) + "%26" + message.substring(i+1);
			}
			while(true){
				i = message.indexOf('=');	if(i < 0){break;}
				message = message.substring(0,i) + "%3D" + message.substring(i+1);
			}
			while(true){
				i = message.indexOf('+');	if(i < 0){break;}
				message = message.substring(0,i) + "%2B" + message.substring(i+1);
			}*/
			//message = message.replace( ',', '\n');
			message = com.j_phone.io.URLEncoder.encode(bboard.getString());
			name = btitle.getString();
			mail = bres.getString();
			//�������݃f�[�^�쐬
			if(strdata[0].indexOf("kakiko.com") != -1){
				strdata[5] = "key=" + th + "&FROM=" + com.j_phone.io.URLEncoder.encode(name) + "&mail=" + com.j_phone.io.URLEncoder.encode(mail) + "&MESSAGE=" + message + "&submit=%8f%91%82%ab%8d%9e%82%de&bbs=" + bbsname + "&time=" + th/*System.currentTimeMillis()/1000*/  + "&get=1&MIRV=kakkoii";
				//sbuf.append("key=" + th + "&FROM=" + name + "&mail=" + mail + "&MESSAGE=" + message + "&submit=��������&bbs=" + bbsname + "&time=" + th/*System.currentTimeMillis()/1000*/  + "&get=1&MIRV=kakkoii");
				//StringBuffer sbuf = new StringBuffer("key=" + th + "&FROM=" + name + "&mail=" + mail + "&MESSAGE=" + message + "&submit=��������&bbs=" + bbsname + "&time=" + th/*System.currentTimeMillis()/1000*/  + "&get=1&MIRV=kakkoii");
			}else if(strdata[0].indexOf("vip2ch.com") != -1){
				strdata[5] = "key=" + th + "&FROM=" + com.j_phone.io.URLEncoder.encode(name) + "&mail=" + com.j_phone.io.URLEncoder.encode(mail) + "&MESSAGE=" + message + "&bbs=" + bbsname + "&time=" + th + "&suka=pontan&submit=%8F%E3%8BL%91S%82%C4%82%F0%8F%B3%91%F8%82%B5%82%C4%8F%91%82%AB%8D%9E%82%DE";
//				sbuf.append("key=" + th + "&FROM=" + name + "&mail=" + mail + "&MESSAGE=" + message + "&submit=��&bbs=" + bbsname + "&time=" + th + "&get=1&hana=mogera");
				//StringBuffer sbuf = new StringBuffer("key=" + th + "&FROM=" + name + "&mail=" + mail + "&MESSAGE=" + message + "&submit=��&bbs=" + bbsname + "&time=" + th + "&get=1&hana=mogera");
			}else{
				strdata[5] = "key=" + th + "&FROM=" + com.j_phone.io.URLEncoder.encode(name) + "&mail=" + com.j_phone.io.URLEncoder.encode(mail) + "&MESSAGE=" + message + "&submit=%8f%91%82%ab%8d%9e%82%de&bbs=" + bbsname + "&time=" + th + "&get=1";
//				sbuf = "key=" + th + "&FROM=" + name + "&mail=" + mail + "&MESSAGE=" + message + "&submit=��&bbs=" + bbsname + "&time=" + th + "&get=1");
				//StringBuffer sbuf = new StringBuffer("key=" + th + "&FROM=" + name + "&mail=" + mail + "&MESSAGE=" + message + "&submit=��&bbs=" + bbsname + "&time=" + th + "&get=1");
			}
		} else {// if(mode != 0){
			StringBuffer sbuf = new StringBuffer("v=D");
			if(mode == 0 || mode == 3){
				data[78] = 0x00000008;	//��ʂ̃_�E�����[�h
			} else {
				Bugln("ThreadView...");
				data[78] = 0x00000002;	//�X���b�h���X�g
				if(mode == 5){
					data[78] = 0x00000004;	//�T�[�o�[���I��
					//strdata[5] = strdata[5] + "&m=u&u=" + strdata[6] + "&c=b" + data[0] + "r" + data[1];
					sbuf.append("&m=u&u=");
					sbuf.append(strdata[6]);
					sbuf.append("&c=b");
					sbuf.append(data[0]);
					sbuf.append("r");
					sbuf.append(data[1]);
					//stat3 |= 0x0000004;
				} else if(mode != 4){
					//strdata[5] = strdata[5] + "&b=" + brd;
					sbuf.append("&b=");
					sbuf.append(brd);
					if(to == 0){
						if(mode == 1){to = st + data[0] - 1;}
						if(mode == 2 && st > 0){to = st + data[1] - 1;}
					}

					if(st > 0 && ! (st == 1 && to == 10)){
						//strdata[5] = strdata[5] + "&c=s" + st + "t" + to;
						sbuf.append("&c=s");
						sbuf.append(st);
						sbuf.append("t");
						sbuf.append(to);
					} else if(st < 0){//�ŐV���X
						if(to == 0){to = data[1];}
						//strdata[5] = strdata[5] + "&c=l" + to;
						sbuf.append("&c=l");
						sbuf.append(to);
					}
				} else {	//�X������
					Bugln("ThreadSearch...");
					//strdata[5] = strdata[5] + "&m=s&b=" + brd + "&w=" + strdata[6];
					sbuf.append("&m=s&b=");
					sbuf.append(brd);
					sbuf.append("&w=");
					sbuf.append(strdata[6]);
				}
				if(mode == 2){	//���X�\���̎��̂�
					Bugln("ResView...");
					data[78] = 0x00000001;	//���X
					//strdata[5] = strdata[5] + "&t=" + th;
					sbuf.append("&t=");
					sbuf.append(th);
				}
			}
			//if(mode == 1 || mode == 2 || mode == 4 || mode == 5 || mode == 6 || mode == 7){
			//if(mode != 7){
			if(data[76] == 1){sbuf.append("&p=p1");}//strdata[5] = strdata[5] + "&p=p1";}	//��t�̂�
			else {sbuf.append("&p=p3");}//strdata[5] = strdata[5] + "&p=p3";}	//���k�Ȃ�



			if(data[80] == 6){	//gzip
				//strdata[5] = strdata[5] + "sd";
				sbuf.append("sd");
				data[78] |= 0x00000100;	//gzip���k�w��
			} else if(data[80] != 0) {
				//strdata[5] = strdata[5] + "sd" + data[80];
				sbuf.append("sd");
				sbuf.append(data[80]);
				data[78] |= 0x00000100;	//gzip���k�w��
			}


			//}
			if(mode == 2 || mode == 5){	//���X�\���̎��̂�
				if((data[49] & 0x04) == 0){sbuf.append("i");}//strdata[5] = strdata[5] + "i";}
				if((data[49] & 0x08) == 0){sbuf.append("t");}//strdata[5] = strdata[5] + "t";}
				if((data[49] & 0x10) == 0){sbuf.append("m");}//strdata[5] = strdata[5] + "m";}
				if((data[49] & 0x40) != 0){sbuf.append("n");}//strdata[5] = strdata[5] + "n";}

				i = data[87] & 0xFF;
				if(i == 2){sbuf.append("a");//strdata[5] = strdata[5] + "a";
				} else if(i > 2){sbuf.append("a" + i);}//strdata[5] = strdata[5] + "a" + i;}
				//if((data[57] & 0x00000002) == 0){strdata[5] = strdata[5] + "a";}

				i = (data[87] & 0xFF00) >> 8;
				if(i == 2){sbuf.append("u");//strdata[5] = strdata[5] + "u";
				} else if(i > 2){sbuf.append("u" + i);}//strdata[5] = strdata[5] + "u" + i;}
				//if((data[57] & 0x00004000) != 0){strdata[5] = strdata[5] + "u2";
				//} else if((data[57] & 0x00000004) == 0){strdata[5] = strdata[5] + "u";}

				if((data[57] & 0x00002000) != 0){sbuf.append("r");}//strdata[5] = strdata[5] + "r";}	//�����폜�΍�
			}
			//�g���I�v�V����
			//strdata[5] = strdata[5] + extendedoption;
			sbuf.append(extendedoption);
			strdata[5]=sbuf.toString();
			sbuf = null;
		}

//#ifdef DEBUG
//		System.out.println(server + "2.cgi?"+strdata[5]);
//#endif

		//data[82] = data[82] * 10;
		///System.out.println("1/3�{��:" + data[82]);
		if(mode == 0){
//#ifdef DOJA	//DOJA
			stat3 |= 0x0100000;	//�ꗗ�̎擾
			stat |= 0x0010;	//�ʐM
			//thttpget();
//#endif
		} else if(mode == 6 || mode == 7) {
			data[78] = 0x00000208;	//��ʂ̃_�E�����[�h,iMona�w�b�_����
			if(mode == 6){
				Bugln("Other...");
				//data[78] = 4;	//��ʂ̃_�E�����[�h(�w�b�_����)

				data[79] = brd;
				strdata[1] = null;
			} else if(mode == 7){//��������
				Bugln("Writing...\n");
				data[78] |= 0x00000010;	//��������
				//data[78] = 5;	//��������
			}/* else if(mode == 13){//AAS
				data[79] = brd;
				strdata[1] = null;
				data[72] = 1;
			}*/
			Thread thread = new Thread() {
				public final void run() {
					//stat |= 0x0001000;	//�ꗗ�擾��
					stat |= 0x0010;	//�ʐM
					httpgetdata();
					if((stat2 & 0x0001000) == 0){	//�G���[���b�Z�[�W���o�Ă��Ȃ��Ƃ�
						//strdata[7] = new String(dlarray);
						//stat2 |= 0x0001000;
						if((data[78] & 0x00000010) != 0){//��������
							Bugln("Ok!\n");
							stat2 |= 0x0001000;
							if((new String(dlarray)).indexOf("�������݂܂���") != -1 || (new String(dlarray)).indexOf("<!-- 2ch_X:true -->") != -1 || (new String(dlarray)).indexOf("<!-- 2ch_X:false -->") != -1){
								strdata[7] = "��������";
								Bugln("Writing=Complete!\n");
								bodytext = "";
								//addNamelist(btitle.toString());
							} else if((new String(dlarray)).indexOf("�K��") != -1){
								strdata[7] = "�������s(�K��)";
								Bugln("Writing=Kiyaku!\n");
								viewwritepanel();
							} else if((new String(dlarray)).indexOf("�d�q�q�n�q") != -1) {
								String errortext;
								int start, end;
								errortext = new String(dlarray);
								//<b>�d�q�q�n�q�F�{��������܂���I</b>
								start = errortext.indexOf("<b>") + 3 + 12;
								end  = errortext.indexOf("</b>");
								strdata[7] = "�������s\n" + errortext.substring(start,end);
								Bugln("Writing=Error!\n");
								Bugln(errortext + "\n");
								//strdata[7] = "�������s:" + errortext.substring(start,end);
								viewwritepanel();
							} else {
								Bugln("Writing=UnknownError!\n");
								strdata[7] = "�����s���̏������s";
								/*try{
									String buf;
									buf = new String(dlarray);
									tbox = new LocalizedTextBox("�����s���ȏ������s", buf.toString(), buf.length(), LocalizedTextField.ANY);
									tbox.addCommand(command[2]);
									stat3 |= 0x0002000;
									tbox.setCommandListener(parent.canv);
									disp.setCurrent(tbox);
								} catch(Exception e){
								}*/
								viewwritepanel();
							}
							//stat2 |= 0x0001000;
						}/* else if(data[72] == 1){ //AAS
							strdata[1] = new String(dlarray);
							strdata[0] = strdata[1];
							if(strdata[0].charAt(strdata[0].length() - 1) == '/'){
								strdata[0] = strdata[0].substring( 0, strdata[0].length() - 1);
							}
							int i = strdata[0].lastIndexOf( '/', strdata[0].length() - 1);
							bbsname = strdata[0].substring(i + 1);
							strdata[0] = strdata[0].substring( 0, i);
							String buf = "example.ddo.jp/aas/a.i/" + strdata[0] + "/" + bbsname + "/" + nCacheTh[nCacheIndex][0] + "/" + (data[6] + nCacheSt[nCacheIndex]);
							data[72] = 0;
							openbrowser(buf, 1);
							data[78] &= ~0x00000300;
						}*/else {//if(data[78] == 4){
							strdata[1] = new String(dlarray);
							if((stat4 & 0x0040000) != 0){
								stat4 ^= 0x0040000;
								viewwritepanel();
								Bugln("WritingUrlGet\n");
							}else{
								Bugln("AASUrlGet\n");
								String buf = "";
								strdata[0] = strdata[1];
								if(strdata[0].charAt(strdata[0].length() - 1) == '/'){
									strdata[0] = strdata[0].substring( 0, strdata[0].length() - 1);
								}
								int it = strdata[0].lastIndexOf( '/', strdata[0].length() - 1);
								bbsname = strdata[0].substring(it + 1);

/*								if((data[78] & 0x0000800) != 0){//c.2ch
									data[78] &= ~0x0000800;
									buf = "c.2ch.net/test/-/" + bbsname + "/" + nCacheTh[nCacheIndex][0] + "/" + (data[6] + nCacheSt[nCacheIndex]) + "-";
								}else/* if((data[78] & 0x0000800) != 0)*///{//AAS*/
									//data[78] &= ~0x0000400;
									strdata[0] = strdata[0].substring( 0, it);
									buf = "example.ddo.jp/aas/a.i/" + strdata[0] + "/" + bbsname + "/" + nCacheTh[nCacheIndex][0] + "/" + (data[6] + nCacheSt[nCacheIndex]);

								//}
								openbrowser(buf,1);
							}
						}

					}
					stat |= 0x1000000;	//��ʍX�V
					//stat &= ~0x0001000;	//�ꗗ�擾��
				}
			};
			thread.start();
		} else {

			Linkref[0] = brd;
			Linkref[1] = th;
			Linkref[2] = st;
			//Linkref[0] = to
			strdata[10] = strdata[8];
//#ifdef DEBUG
//			System.out.println(brd + " " + th + " " + st);
//#endif
			stat |= 0x0010;	//�ʐM
			stat &= ~0x40020000;	//Loading���̑���͕s�\(0x40000000)�ALoading���̃X���ǂ݂�����
			if((stat3 & 0x0000080) != 0){//��ǂ݂����邱�Ƃ������t���O
				stat3 &= ~0x0000080;	//��ǂ݂����邱�Ƃ������t���O
				stat |= 0x0020000;	//Loading���̃X���ǂ�
			}
		}
//		System.out.println(server + "2.cgi?"+strdata[5]);
		stat2 &= ~0x0004000;	//function����
		stat |= 0x1000000;	//��ʍX�V
	}

	/**
	 * ������ʂ�\��
	 */
	public final void viewwritepanel(){
		int i;
		//String[] namedata = new String[10];
		strdata[0] = strdata[1];
		if(strdata[0].charAt(strdata[0].length() - 1) == '/'){
			strdata[0] = strdata[0].substring( 0, strdata[0].length() - 1);
		}
		i = strdata[0].lastIndexOf( '/', strdata[0].length() - 1);
		bbsname = strdata[0].substring(i + 1);
		strdata[0] = strdata[0].substring( 0, i);
		Bugln("*WritingUrlState\n" + "strdata[0]:" + strdata[0] + "\nbbsname:" + bbsname + "\ni:" + i + "\n");
		btitle = new LocalizedTextField("���O",name,1024,LocalizedTextField.ANY);
		bres = new LocalizedTextField("E-mail",mail,1024,LocalizedTextField.ANY);
		bboard = new LocalizedTextField("���e",bodytext,4096,LocalizedTextField.ANY);
//		bthread = new LocalizedTextField("�ڔԍ�(�ύX�s�v)",""+BookMarkData[i * 3 + 1],16,LocalizedTextField.NUMERIC);
		inputForm = new Form("��������");
		inputForm.append(btitle);
		inputForm.append(bres);
		inputForm.append(bboard);
		//inputForm.append(new StringItem("�u,�v�ŉ��s���邱�Ƃ��ł��܂�",""));
		if( (stat & 0x40000) != 0 ){	//ڽ���Ă鎞
			inputForm.append(new LocalizedTextField("URL��",">>" + (data[6] + nCacheSt[nCacheIndex]) + "\n" + strdata[9] + "\nhttp://" + strdata[0] + "/test/read.cgi/" + bbsname + "/" + nCacheTh[nCacheIndex][0] + "/" + "\nhttp://c.2ch.net/test/-/" + bbsname + "/" + nCacheTh[nCacheIndex][0] + "/i",500,LocalizedTextField.ANY));
		} else if((stat2 & 0x0010000) != 0){	//�u�b�N�}�[�N
			inputForm.append(new LocalizedTextField("URL��",tBookMark[(data[67] + data[68])] + "\nhttp://" + strdata[0] + "/test/read.cgi/" + bbsname + "/" + BookMarkData[(data[67] + data[68]) * 3 + 1] + "/" + "\nhttp://c.2ch.net/test/-/" + bbsname + "/" + BookMarkData[(data[67] + data[68]) * 3 + 1] + "/i",500,LocalizedTextField.ANY));
		}

		//namedata = loadNamelist(0);
		//inputForm.append(new LocalizedTextField("���O����",namedata[0],4096,LocalizedTextField.ANY));
//		inputForm.append(bthread);
		inputForm.addCommand(command[8]);
		inputForm.addCommand(command[2]);
		inputForm.setCommandListener(this);
		disp.setCurrent(inputForm);
		stat2 |= 0x20000000;
	}
//	public final void write(){
//	}
	//SO SC
	//1  1  > OFF
	//1  0  > OFF
	//0  1  > ON
	//0  0  > OFF

	/**
	 * �X�N���[������
	 */
	public final void Scroll(){
//		int i = data[60] + data[65];
		if( (stat & 0x0300000) != 0 ){//�㉺�X�N���[��ON
			int k = 0, n = 0, m, i;
			data[95] = 0;
			//stat3 |= 0x0010000;	//�X�N���[������
			if(data[60] != -1){
				k = data[30] + data[34];//����+�s��
				n = data[77] / k;//	if(0 > n){n = 0;}	//DivStr�̏����n��
				k = (height + data[77] - data[30] - 4) / k + 1;	//����ȏ�͕\������Ȃ��̂ŏ����Ȃ�
				//if(k > data[85]){k = data[85];}	//index����͂���Ȃ����߂�
			}
			if(data[59] > 0){
				i = (Linklist[data[60]] / 1000) % 10000;	//���݃t�H�[�J�X���������Ă��郊���N�̍s
			} else {i = k;}
			//System.out.println("i:" + i + " n:" + n + " k:" + k + " 88:" + data[88]);
			//i:���݃t�H�[�J�X���������Ă��郊���N�̍s
			//m:��Oor���̃����N�̍s
			//n:�����n��
			//k:����ȏ�͕\������Ȃ��̂ŏ����Ȃ�

			if( (stat & 0x0100000) != 0 ){//��X�N���[��ON
	//			if(data[65] > 0){	//Link�̑I���X�L�b�v��
	//				data[65]--;
	//				stat |= 0x1000000;	//��ʍX�V
				if(/*data[60] != -1 && */data[60] > 0){
					m = (Linklist[data[60]-1] / 1000) % 10000;
					if(data[88] == 0 || i > k){
						if( ((data[57] & 0x00028000) != 0 && (stat & 0x0010000) != 0) ||
						 (/*m == 0 || */m > n || (data[77] == 0 && (stat & 0x0040000) == 0) || (data[77] == -data[30] && (stat & 0x0040000) != 0))/* || m <= k*/){	//�����N����Ƃ���͕\�����ɂ���ꍇ
							data[60]--;	i = m;
							data[88] = -3;
							//stat |= 0x1000000;	//��ʍX�V
							//return;
						}
					} else if(data[88] > 0) {
						data[88] = -3;
					} else {
						data[88]++;
					}
				}
				if((data[57] & 0x00028000) != 0 && (stat & 0x0010000) != 0){
					if((data[57] & 0x00008000) != 0 || i < n){
						data[77] = i/*((Linklist[data[60]] / 1000) % 10000)*/ * (data[30] + data[34]);
					}
				} else if((data[77] > 0 || (data[77] > -data[30] && (stat & 0x0040000) != 0)) ){
					if( (data[49] & 0x20) != 0 || (data[49] & 0x01) == 0 ){stat ^= 0x0100000;}
					data[77] -= data[35];
					if((stat & 0x0040000) != 0){
						if(data[77] < -data[30]){data[77] = -data[30];}
					} else if(data[77] < 0){data[77] = 0;}
	/*				if(i > 1 && (Linklist[i] / 1000) % 10000 == (Linklist[i - 1] / 1000) % 10000){
						data[65]--;
					} else {
						data[65] = 0;
					}
	*/
					//repaint();	continue;//������
				}
				//stat |= 0x1000000;	//��ʍX�V
			} else if( (stat & 0x0200000) != 0 ){//���X�N���[��ON
			//System.out.println("i:" + i + " " + data[59] + " " + data[65] + " " + data[60]);
	//			if(data[60] != -1 && i + 1 < data[59] && (Linklist[i] / 1000) % 10000 == (Linklist[i + 1] / 1000) % 10000){
	//				data[65]++;
	//				stat |= 0x1000000;	//��ʍX�V
				if(data[60] != -1 && data[60] + 1 < data[59]){
					m = (Linklist[data[60]+1] / 1000) % 10000;
					if(data[88] == 0 || i < n){
						if(/*(m == 0 || m > n) &&*/ m <= k-5/*k-1*/ || ((data[57] & 0x00028000) != 0 && (stat & 0x0010000) != 0)){	//�����N����Ƃ���͕\�����ɂ���ꍇ
							data[60]++;	i = m;
							data[88] = 3;
							//stat |= 0x1000000;	//��ʍX�V
							//return;
						}
						//m = (Linklist[data[60]] / 1000) % 10000;
						//if(m <= k-3){
						//	if(m > n/* || m <= k*/){	//�����N����Ƃ���͕\�����ɂ���ꍇ
						//		if(m == (Linklist[data[60] + 1] / 1000) % 10000){
						//			data[60]++;
						//			//stat |= 0x1000000;	//��ʍX�V
						//			//return;
						//		}
						//	} else {
						//		data[60]++;
						//	}
						//}
					} else if(data[88] < 0) {
						data[88] = 3;
					} else {
						data[88]--;
					}
				}
				if((data[57] & 0x00028000) != 0 && (stat & 0x0010000) != 0){
					if(data[60] + 1 < data[59]){
						m = (Linklist[data[60]+1] / 1000) % 10000;
					} else {
						m = i + 3;
					}
					if((data[57] & 0x00008000) != 0 || m > k-3){	//�X�����Ƃ̃X�N���[��
						data[77] = i/*((Linklist[data[60]] / 1000) % 10000)*/ * (data[30] + data[34]);
					}
				} else if((data[77] < (data[85] + 5/*2*/) * (data[30] + data[34]) - height)){
	//				data[65] = 0;
					if( (data[49] & 0x20) != 0 || (data[49] & 0x01) == 0 ){stat ^= 0x0200000;}
					//if((stat & 0x0010000) != 0 && data[35] > data[30] + data[34]){	//�X���I�� & �X�N���[���ʂ������̍����𒴂���Ƃ�
					//	data[77] += data[30] + data[34];
					//} else {
						data[77] += data[35];
					//}
					//stat |= 0x1000000;	//��ʍX�V
					//repaint();	continue;//������
	//			} else if((data[60]/* + data[65]*/) < data[59]-1){	//Link�̑I���X�L�b�v��
	//				data[65]++;
	//				data[60]++;
	//				stat |= 0x1000000;	//��ʍX�V
				}
			}
			stat |= 0x1000000;	//��ʍX�V
		} else if( (stat2 & 0x0400000) != 0 ){//���X�N���[��ON
			data[95] = 0;
			//stat3 |= 0x0010000;	//�X�N���[������
			if(data[48] < 0){
				if( (data[49] & 0x01) == 0 ){stat2 ^= 0x0400000;}
				data[48] += data[35];
				if(data[48] > 0){
					data[48] = 0;
				}
				stat |= 0x1000000;	//��ʍX�V
			}
		} else if( (stat2 & 0x0800000) != 0 ){//�E�X�N���[��ON
			data[95] = 0;
			//stat3 |= 0x0010000;	//�X�N���[������
			if( (data[49] & 0x01) == 0 ){stat2 ^= 0x0800000;}
			data[48] -= data[35];
			stat |= 0x1000000;	//��ʍX�V
		}
	}

	/**
	 * �ҋ@����
	 * @param w �~���b�őҋ@����
	 */
	public final void wait(int w){
		try{Thread.sleep(w);}catch (Exception e){}
	}

	/**
	 * ran�B������ŏ��Ɏ��s����郁�\�b�h�ł����̒������邮����
	 */
	public final void run(){
		//int s = 200;
		while(true){
//			try{
			//if(((stat & 0x0010) != 0 || (stat & 0x10000000) != 0) && (stat & 0x0300000) == 0/*scroll*/ && (stat & 0x1000000) == 0){	//�ʐM��or�ʐM��̏�����
			if((stat & 0x10000010) != 0 && (stat & 0x1300000) == 0){	//�ʐM��or�ʐM��̏�����
//�����Ȓ[���ł̓X���[�v���Ԃ�Z�����āA�X�N���[���̔������x���グ��

				wait(data[82] * 10);	//10�{�X���[�v
				if(data[95] >= 0){
					data[95] += data[82];
				}
				stat |= 0x2000000;	//�g�b�v�̂ݍX�V
			} else {
				wait(data[82]);
				if(data[95] >= 0){
					data[95] += data[82];
				}
			}
//			}catch (Exception e){
//			}

			//5�b�ȏ㑀�삪�Ȃ��Ƃ��͓d�r�A�d�g����\������B
			if(data[97] != 0){
				if(data[95] > data[97]){
					data[95] = -1;
					stat |= 0x1000000;	//��ʍX�V
				}
			}
			//if( (stat & 0x10000000) != 0 ){	//�ʐM��̏�����(�ʐM�͊���)
			//	stat |= 0x2000000;	//�g�b�v�̂ݍX�V
			//}
			if( (stat & 0x0450000) != 0/* && (stat & 0xF000000) == 0*/){	//���X�\����
				Scroll();	//ڽ��۰�
			}

			if( (stat & 0xF000000) != 0){	//�ĕ`��
				repaint();
			}
			if( (data[49] & 0x02) != 0){	//�b�\��
				repaint();
			}
		}
	}
	/**
	 * ���ۂ̒ʐM����
	 * mode(data[78]) - 1:���X�̎擾(3:+�^�C�g��) 2:�X���ꗗ�̎擾(4:+�^�C�g��)
	 * @see MainCanvas#httpinit
	 * @see MainCanvas#thttpget
	 */
	public final void httpgetdata(){
		int i, j = 0, k = 0, n = 0;
		//int mode = 0;//1:���X�̎擾(3:+�^�C�g��) 2:�X���ꗗ�̎擾(4:+�^�C�g��)
		//int t1 = 0, t2 = 0;//�e���|���� t1:�X���ԍ� t2:�ԍ�

		int rc = 0;	//response code

		while( (stat & 0x10000000) != 0 ){}
		//System.out.println("http");
		if( (stat & 0x0010) == 0 ){	//�ʐM���ł͂Ȃ�
			return;
		}
		int mode = data[78];

		//String url = new String("http://localhost:81/i2ch/2.cgi");
		//HttpConnection co = null;
		InputStream in = null;
//#ifdef DOJA505
//		JarInflater jarinf;
//#endif
		int errorcode = 0;	//�ʐM�̃G���[�R�[�h
		//InputStream����ByteArrayOutputStream�֓���ւ�
		ByteArrayOutputStream outstr = new ByteArrayOutputStream();
		try {
			byte d1[] = new byte[1];
			//int len;
			data[14] = 0;	data[15] = 0;	data[16] = 0;	data[17] = 0;	data[18] = 0;
			///System.out.println("CON:START!");
			//�ʐM��`
			//HttpConnection co = (HttpConnection)Connector.open(IApplication.getCurrentApp().getSourceURL() + "2.cgi",Connector.READ_WRITE,true);
			//HttpConnection co = (HttpConnection)Connector.open("http://localhost:81/i2ch/2.cgi",Connector.READ_WRITE,true);

			if((mode & 0x00000010) != 0){	//��������
				Bugln("WritingUrl:" + "http://" + strdata[0] + "/test/bbs.cgi\n");
				co = (HttpConnection)Connector.open("http://" + strdata[0] + "/test/bbs.cgi",Connector.READ_WRITE,true);
			} else {
				co = (HttpConnection)Connector.open(server + "2.cgi",Connector.READ_WRITE,true);
			}
//#ifdef DEBUG
//				System.out.println("CONNECTION:OPEN");
//#endif
			co.setRequestMethod(HttpConnection.POST);

			//HTTP�w�b�_����������
			if((mode & 0x00000010) != 0){	//��������
				if( (stat & 0x40000) != 0 ){	//ڽ���Ă鎞
					Bugln("WritingReferer:" + "http://" + strdata[0] + "/test/read.cgi/" + bbsname + "/" + nCacheTh[nCacheIndex][0] + "/\n");
					co.setRequestProperty("Referer",
							"http://" + strdata[0] + "/test/read.cgi/" + bbsname + "/" + nCacheTh[nCacheIndex][0] + "/");
				} else if((stat2 & 0x0010000) != 0){	//�u�b�N�}�[�N
					Bugln("WritingReferer:" + "http://" + strdata[0] + "/test/read.cgi/" + bbsname + "/" + BookMarkData[(data[67] + data[68]) * 3 + 1] + "/\n");
					co.setRequestProperty("Referer",
							"http://" + strdata[0] + "/test/read.cgi/" + bbsname + "/" + BookMarkData[(data[67] + data[68]) * 3 + 1] + "/");
				}
				co.setRequestProperty("User-Agent",
						"Monazilla/1.00");
				co.setRequestProperty("Cookie", 
						cookie);
				

				/*if(strdata[0].indexOf("vip2ch.com") != -1){

				}*/
			}
			//data[16] = 0;
			//OutputStream�֑���f�[�^������
			OutputStream out = co.openOutputStream();
			out.write(strdata[5].getBytes());
			out.close();
			stat |= 0x0020;	//�ڑ�����	httpstat = 1;
			if((stat & 0x0080000) != 0){//�ʐM�X�g�b�v�v��
				throw new Exception();
			}
//#ifdef
			rc = co.getResponseCode();
//#endif
			errorcode++;	//�ʐM�̃G���[�R�[�h1
			if((stat & 0x0080000) != 0 || rc >= 300){//�ʐM�X�g�b�v�v��or���X�|���X�R�[�h��300�ȏ�
				throw new Exception();
			}
			//InputStream�փ��X������
			in = co.openInputStream();
			stat |= 0x0040;	//�ڑ���������M��	httpstat = 2;
//#ifdef DEBUG
//				System.out.println("CONNECTION:INPUTOPEN");
//#endif
			errorcode++;	//�ʐM�̃G���[�R�[�h2
			System.gc();	//�K�x�[�W�R���N�V����

			if(data[80] != 0 && (mode & 0x00000100) != 0/*(data[78] == 0 || data[78] == 1 || data[78] == 2)*/){	//gzip���k�w�肪����ꍇ
				in.read(d1);
				if(d1[0] == 0x01){
					i = 1;
					while(true){	//�e�ʂ̎擾
						if(in.read(d1) < 0){break;}
						i++;
						if(d1[0] == 0x0A){//\n
							data[17] = to10(outstr.toByteArray()) + i;
							outstr.reset();
							break;
						} else {
							outstr.write(d1);
						}
					}
					//P4,P5�̋@�\���g�p����
					in = new InflateInputStream(in);
				}
			}
			errorcode++;	//�ʐM�̃G���[�R�[�h3
			if((mode & 0x00000200) == 0/*data[78] != 4 && data[78] != 5*/){	//�w�b�_�t���̒ʐM�̏ꍇ

				if(in.read(d1) < 0){
					throw new Exception();
				}
				if((mode & 0x00000004) != 0/*data[78] == 0*/){	//�T�[�o�[���ŁA�X���b�h�̓ǂݍ��݂��̓ǂݍ��݂������߂�ꍇ
					mode ^= 0x00000004;
					//if(d1[0] == 0x01 || d1[0] == 0x03){	//���X
					//	mode = 1;
					i = 0;
					//if(d1[0] == 0x01 || d1[0] == 0x03){	//���X
					if(d1[0] == 0x13){	//���X
						mode |= 0x00000001;
						//stat &= ~0x0040000;	//���X�\�����̂Ƃ��͉���
						//stat3 |= 0x0000080;	//�ꗗ������(�ēǍ����K�v)
					//} else if(d1[0] == 0x02 || d1[0] == 0x04){	//�X���b�h���X�g
					} else if(d1[0] == 0x14){	//�X���b�h���X�g
						mode |= 0x00000002;
					}
					//���̓�ȊO�̓G���[
					//stat |= 0x0020000;	//���X�\����(+���X�����擾��)
					if((mode & 0x00000003) != 0){	//�G���[�łȂ��ꍇ
						while(true){
							if(in.read(d1) < 0){break;}
							if(d1[0] == 0x0A){
								strdata[10] = outstr.toString();	//�X���b�h�^�C�g��
								if(strdata[10].length() == 0){
									strdata[10] = "?";
								}
//#ifdef DEBUG
//								System.out.println("title:" + strdata[8]);
//#endif
								break;
							} else if(d1[0] == '\t'){
								if(i == 0){
									Linkref[0] = to10(outstr.toByteArray());	//�ԍ�
								} else {
									Linkref[1] = to10(outstr.toByteArray());	//�X���i���o�[
								}
								i++;
								outstr.reset();
							} else {
								outstr.write(d1);
							}
						}
						in.read(d1);
					}
	/*
					} else if(d1[0] == 0x02 || d1[0] == 0x04){	//�X���b�h���X�g
					//	mode = 2;
						//stat |= 0x8000;	//�X���ꗗ�c�k��
						data[78] = 2;
						while(true){
							if(in.read(d1) < 0){break;}
							if(d1[0] == 0x0A){
								strdata[3] = outstr.toString();	//��
								break;
							} else if(d1[0] == '\t'){
								Linkref[0] = to10(outstr.toByteArray());	//�ԍ�
								outstr.reset();
							} else {
								outstr.write(d1);
							}
						}
					}
	*/
				}
				outstr.reset();
//#ifdef
//				errorcode++;	//�ʐM�̃G���[�R�[�h4
//#endif
				k = 1;	data[15]++;
				if(0 <= d1[0] && d1[0] <= 0x0F){
					if(d1[0] == 0x00/*'0'*/){
						strdata[7] = "�Ȃ�炩�̴װ";//StrList[10][5];
					} else if( d1[0] == 0x01/*'A'*/ ){	//�V���X���Ȃ�
						strdata[7] = "�Vڽ�͂���܂���";//StrList[10][4] + StrList[10][18];
						Bugln("Ok\n");
					} else if( d1[0] == 0x02/*'B'*/ ){	//spdv->2ch�ւ̐ڑ��G���[
						strdata[7] = "�ʐM�װ(2ch)";
						Bugln("Ok\n");
					} else if( d1[0] == 0x03 ){	//�����ŉ���������Ȃ������Ƃ�
						strdata[7] = "No hit...";
						Bugln("Ok\n");
					} else if( d1[0] == 0x04 ){	//DAT����
						strdata[7] = "DAT����";
						Bugln("Ok\n");
					} else if( d1[0] == 0x05 ){	//�l����
						strdata[7] = "�l����";
						Bugln("Ok\n");
					} else if( d1[0] == 0x06){//���ԎI�ړ]&�폜
						strdata[7] = "���ԎI���ړ]�����݂���";
					} else {// if( 0x06 <= d1[0] && d1[0] <= 0x0F ){	//�T�|�[�g����Ă��Ȃ��G���[
						strdata[7] = "���̑��̴װ";//StrList[10][4] + StrList[10][18];
						Bugln("Ok\n");
					}
					stat2 |= 0x0001000;
				} else {
					Bugln("Ok\n");
					//�L���b�V���ɒǉ�
					if((mode & 0x00000003) != 0){
						//�S�ẴL���b�V����TTL��1���₵�ATTL����ԑ傫�����͖����ł���L���b�V����I������
						j = 0;	n = 0;
						i_length = CacheTitle.length;
						for(i = 0;i < i_length;i++){
							//nCacheTTL[i]++;
							if(j == -1){
							} else if(nCacheTTL[i] == -1){
								j = -1;	n = i;
							} else if(nCacheTTL[i] > j){
								j = nCacheTTL[i];	n = i;
							}
						}
						nCacheTTL[n] = -1;	//�L���b�V����ҏW���邽�߂��̃L���b�V���𖳌��ɂ���
						if((mode & 0x00000001) != 0){	//���X�̃_�E�����[�h
							//CacheResData[n] = Res;
							CacheBrdData[n] = null;
							nCacheBrdData[n] = null;
							nCacheTh[n] = new int[1];	nCacheTh[n][0] = Linkref[1];
							//nCacheSt[n] = data[7];
							//nCacheTo[n] = data[8];
							//nCacheAll[n] = data[9];
						} else {//if(mode == 2){	//�X���ꗗ�_�E�����[�h��
							searchtext = "";
							CacheResData[n] = null;
							//CacheBrdData[n] = ThreadName;
							//nCacheBrdData[n] = nRes;
							//nCacheTh[n] = nThread;
							//nCacheSt[n] = Linkref[2];
							//nCacheTo[n] = Linkref[2] + nThread.length - 1;
							nCacheAll[n] = 0;
						}
						nCacheBrd[n] = Linkref[0];
					}
					outstr.write(d1);
					i = 0;
					while(true){	//�e�ʂ̎擾
						if(in.read(d1) < 0){break;}
						k++;	data[15]++;
						if(d1[0] == '\t'){//\t
							j = to10(outstr.toByteArray());
							if( (mode & 0x00000001) != 0/*data[78] == 1*//*((stat & 0x20000) != 0 || (stat & 0x40000) != 0)*/){	//���X�̃_�E�����[�h
								if(i == 0){	data[14] = j;	//len
								} else if(i == 1) {data[18] = j;	//lenbefore
								} else if(i == 2) {nCacheSt[n]/*data[7]*/ = j;	//from
								} else if(i == 3) {nCacheTo[n]/*data[8]*/ = j;	//to
								//} else if(i == 1) {data[9] = to10(outstr.toByteArray());	//all
								}	i++;
							} else {
								data[14] = j;	//len
							}

							outstr.reset();
						} else if(d1[0] == 0x0A){//\n
							j = to10(outstr.toByteArray());
							if( (mode & 0x00000001) != 0/*data[78] == 1*//*((stat & 0x20000) != 0 || (stat & 0x40000) != 0)*/){	//���X�̃_�E�����[�h
								nCacheAll[n]/*data[9]*/ = j;	//all
								//for(j = 0;j < CacheTitle.length;j++){	//�L���b�V�����̌���
								//	if(nCacheTTL[j] >= 0 && CacheBrdData[j] == null && nCacheTh[j] != null && nCacheTh[j][0] == nCacheTh[n][0] && nCacheBrd[j] == nCacheBrd[n]){
								//		nCacheAll[j] = nCacheAll[n];	//All�����ŐV�̂��̂ɍX�V
								//	}
								//}
							} else {
								data[18] = j;	//lenbefore
							}
							outstr.close();
							data[14] += k;
							data[18] += k;
							data[16] = k;//data[14];
							break;
						} else {
							outstr.write(d1);
						}
					}
				}
				if(data[16] == 0){	//DL���s
					throw new Exception();
				}
			}

			outstr = new ByteArrayOutputStream(data[18] + 50);
			errorcode++;	//�ʐM�̃G���[�R�[�h4
			//rbyte = 0;

			//data[14] += data[15] + 1;	//�S�̂̓ǂݍ��ރo�C�g��
			while(true){	//�f�[�^�̎擾 & �P���u���������k & ���������O�X���k��W�J
				if((stat & 0x0080000) != 0){//�ʐM�X�g�b�v�v��
					throw new Exception();
				}
				//len = in.read(d1);
				//if(len <= 0) break;
				if(in.read(d1) <= 0) break;
				data[15]++;
				outstr.write(d1[0]);
				data[16]++;
			}
			errorcode++;	//�ʐM�̃G���[�R�[�h5
			//�ʐM�I������
			in.close();//�ǂݏo���I��
			co.close();//�ʐM�I��
			/*if((mode & 0x00000010) != 0){	//��������
				if(co.getHeaderField("Set-Cookie").indexOf("PON") != -1){
					
				}
			}*/
			///System.out.println("UP:1");
			stat |= 0x0080;	//��M����	httpstat = 4;
		//} catch(UnsupportedEncodingException e) {

		} catch(Exception e) {
			strdata[11] = e.toString();
			try{
				if(in != null){in.close();}
				if(co != null){co.close();}
			} catch(Exception e2) {
			}
			//try{
			//if(in != null)
			//	in.close();
			//in = null;
			//} catch(Exception e2) {
			//}
			///System.out.println("�ʐM�G���[");
			//�ʐM���s
			//stat &= ~0x0020;//if( (stat & 0x0020) != 0 ){stat ^= 0x0020;}
			//stat &= ~0x0040;//if( (stat & 0x0040) != 0 ){stat ^= 0x0040;}
			stat &= ~0x0060;	//�ʐM�G���[�ɂ���
			//stat |= 0x0200;	//test
			//stat |= 0x0080;
		}

		//�󂫃������m��
		in = null;
		co = null;
		
		
		stat3 &= ~0x0000004;	//LinkON(URL)�ŃW�����v�������Ƃ������t���O
	//if((stat & 0x1000) != 0){	//�f�[�^�_�E�����[�h��
		//�X�N���b�`�p�b�h����f�[�^���擾����
	//}
		//�p�P�b�g��̌v�Z
		//calccost();
		//int value = data[15];
		i = data[15];

		if(data[17] != 0){
			//value = data[17];
			i = data[17];
		}

		//if(value < 0)
		//	return;
		i += 500;	//�p�P�b�g��̕␳(�w�b�_�Ƃ�)
		if(data[69] != 0){	//�p�P�b�g��̒P�����ݒ肳��Ă���ꍇ
			data[46] = i * data[69] / 1280;
			data[47] += data[46];
			//return;
		} else {	//�����ݒ�̏ꍇ


			// \0.3/packet
			data[46] = i * 30 / 128;
			data[47] += data[46];
		}
		try {

//			if( (stat & 0x00F0) != 0x00F0 && (stat3 & 0x0000200) != 0){	//DLERROR and �Q�ƌ���ۑ����Ă����ꍇ
//				data[64]--;	//���O�ɕۑ������Q�ƌ���j������
//			}
			if( (stat & 0x00F0) == 0x00F0 ){	//�c�k�I��(����)
				byte[] inbyte;
				zuzu[0] = 0; //���X�ԍ��w���0�ɂ���B

				//�X�����̓��X�̃_�E�����[�h
				if((mode & 0x00000003) != 0){
					CacheTitle[n] = strdata[10];//strdata[8];
					//nCacheTTL[n] = 0;
					//nCacheIndex = n;
				}
				System.gc();	//�K�x�[�W�R���N�V����

				if((mode & 0x00000001) != 0/*data[78] == 1*/){	//���X��(�ǉ�)�_�E�����[�h
					CacheResData[n] = new byte[nCacheTo[n]/*data[8]*/ - nCacheSt[n]/*data[7]*/ + 1][];
					data[45] = 0;//���X���ǂ��܂ŏ���������
				}
				if((mode & 0x00000200) == 0 && (mode & 0x00000001) != 0){//iMona�p�̃w�b�_�L��&���X��DL
					//�Ђ炪�Ȉ��k�̓W�J�E���X����
					inbyte = outstr.toByteArray();	outstr.reset();
					for(i = 0;i < inbyte.length;i++){
						if(inbyte[i] == 0x0A){	//\n


							CacheResData[n][data[45]]/*Res[data[45]]*/ = outstr.toByteArray();	data[45]++;	//���X���ǂ��܂ŏ���������
							outstr.reset();
							if(data[45] == 1 && (stat & 0x0020000) == 0){	//���߂Ẵ��X���������Ă���Ƃ�and�o�b�N�O�����h��M�����Ă��Ȃ�
								stat |= 0x40000000;	//Loading���̑���͕s�\
								if((stat3 & 0x0000010) != 0){	//�߂��Ă�Ƃ�
									data[6] = nCacheTo[n]/*data[8]*/ - nCacheSt[n]/*data[7]*/;
								} else {	//�i��ł���Ƃ�or���X�\�����ł͂Ȃ��ꍇ
									data[6] = 0;
								}
							}
							if(data[45] == data[6] + 1 && (stat & 0x0010) != 0 && (stat & 0x0020000) == 0){	//���X��\���ł���Ƃ���܂ł����Ă�����and�o�b�N�O�����h��M�����Ă��Ȃ�
								stat |= 0x40000000;	//Loading���̑���͕s�\
								nCacheIndex = n;	//���ݓǂ�ł���L���b�V���̃C���f�b�N�X
								//strdata[9] = strdata[8];	//�^�C�g�����X�V
								stat2 |= 0x80000000;	//makeRes�ŏ��������s��
								makeRes();
								stat |= 0x10000000;	//�ʐM��̏�����

								//�I������
								stat &= ~0x0000014;	//�ʐM����and�L�[�X�g�b�v����
								//stat &= ~0x0000004;	//�L�[�X�g�b�v����
								//stat &= ~0x0010;	//�ʐM����
							}
						} else {
							outstr.write(inbyte[i]);
						}
					}
					inbyte = null;
				}
//					if( data[78] == 1 ){	//���X�̃_�E�����[�h
//					} else if(data[78] == 2){	//�X���ꗗ�_�E�����[�h��
				if((mode & 0x00000002) != 0/*data[78] == 2*/){	//�X���ꗗ�_�E�����[�h��
					//�X���ꗗ�̕�������
					inbyte = outstr.toByteArray();
					outstr.reset();
					i = (inbyte[0] & 0xFF) - 16;
					CacheBrdData[n]/*ThreadName*/	= new String[i];
					nCacheBrdData[n]/*nRes*/		= new int[i];
					nCacheTh[n]/*nThread*/			= new int[i];
					byte[] bArray;

					nCacheSt[n] = Linkref[2];
					nCacheTo[n] = Linkref[2] + i - 1;
/*
*/
					j = 0;	k = 0;
					for(i = 1;i < inbyte.length;i++){
						if(inbyte[i] == 0x09){
							if(j == 0){
								nCacheTh[n][k]/*nThread[k]*/ = to10(outstr.toByteArray());	j = 1;
								data[16] += 6;
								outstr.reset();
							} else if(j == 1){
								CacheBrdData[n][k]/*ThreadName[k]*/ = new String( outstr.toByteArray() );
								outstr.reset();
							}
						} else if(inbyte[i] == 0x0A){
							bArray = outstr.toByteArray();
							nCacheBrdData[n][k]/*nRes[k]*/ = to10(bArray);
							data[16] += GetDigits(nCacheBrdData[n][k]/*nRes[k]*/) - bArray.length;
							outstr.reset();
							j = 0;	k++;
						} else {
							outstr.write(inbyte[i]);
						}
					}
					bArray = outstr.toByteArray();
					nCacheBrdData[n][k]/*nRes[k]*/ = to10(bArray);
					data[16] += GetDigits(nCacheBrdData[n][k]/*nRes[k]*/) - bArray.length;
					//�X���ꗗ�Ɉڍs���邽�߂̏���
					if((stat & 0x0020000) == 0){	//�o�b�N�O�����h��M�����Ă��Ȃ�
						stat |= 0x40000000;	//Loading���̑���͕s�\
						if((stat & 0x10000) == 0){	//�X���I�𒆂ł͂Ȃ��ꍇ(�I��or�u�b�N�}�[�N)
							//addCommand(command[9]);//�o�^
							//data[5] = nCacheSt[n] - 1;//Linkref[2] - 1;
							data[60] = 0;
							//data[4] = 0;
							data[77] = 0;
							//data[10] = 0;data[11] = 0;

							//stat ^= 0x8000;	///�X���ꗗ�c�k������
							//if((stat3 & 0x0000020) == 0){	//�u�b�N�}�[�N����ɃW�����v����
							//	data[5] = BookMarkData[(data[67] + data[68]) * 3 + 2];	//���X
							//}
						} else {
							if( (stat & 0x0008000) != 0 ){//�X���ꗗ�̃����[�h
								//data[5] = nCacheSt[n] - 1;//data[5] - data[4];
								data[60] = 0;
								//data[4] = 0;
							} else {
								//if(data[4] == 0){data[5]--;data[4] = nThread.length-1;} else {data[5]++;data[4] = 0;}
								if((stat3 & 0x0000010) != 0){	//���X���Ōォ��ǂނƂ�(�߂��Ă�Ƃ�)
									//data[5]--;
									data[60] = nCacheTh[n].length-1;
									//data[4] = nCacheTh[n]/*nThread*/.length-1;
								} else {	//�i��ł���Ƃ�
									//data[5]++;
									data[60] = 0;
									//data[4] = 0;
								}
							}
						}
						//nCacheTTL[n] = 0;	//�L���b�V����L���ɂ���
						nCacheIndex = n;	//���ݓǂ�ł���L���b�V���̃C���f�b�N�X
						stat2 |= 0x40000000;	//makeTL�ŏ��������s��
						makeTL();
					}
					//stat3 &= ~0x0000080;	//�ꗗ������(�ēǍ����K�v)�������B

				} else if((mode & 0x00000018) != 0/*data[78] == 3 || data[78] == 4 || data[78] == 5*/){
					dlarray = outstr.toByteArray();
				} else if((mode & 0x00000001) != 0){
					data[90] += nCacheTo[n] - nCacheSt[n] + 1;
				}
				outstr.close();
				outstr = null;

				stat &= ~0x00E0;	//�c�k�I��
				if( (stat & 0x0020000) != 0 ){	//�o�b�N�O�����h��M�����Ă���Ƃ�
					nCacheTTL[n] = 0;	//�L���b�V����L���ɂ���
					chkcache();	//�L���b�V���`�F�b�N
				//} else if( (stat3 & 0x0000040) != 0){//�u�߂�v�Ŗ߂��Ă���Ƃ�
				//	data[64]--;
				}
			} else {	//DL ERROR
				if((stat3 & 0x0000200) != 0){//�Q�ƌ���ۑ����Ă����ꍇ
					if(data[64] > 0){data[64]--;}	//���s�����Ƃ��͒��O�ɕۑ������Q�ƌ���j������
				}
				if((stat & 0x0080000) != 0){//�ʐM�X�g�b�v�v��
					strdata[7] = "���f���܂���";
					stat2 |= 0x0001000;
					stat ^= 0x0080000;

				} else if( (stat2 & 0x0001000) == 0 ){	//�܂������\������Ă��Ȃ��ꍇ
					//strdata[7] = null;
					Bugln("Error!\n");
					if(rc == 503){
						StrList[15] = new String[3];
						StrList[15][0] = "�ʐM���s";
						StrList[15][1] = "���ԃT�[�o�[��";
						StrList[15][2] = "����ł��܂��B";
						Bugln("Message:503Error\n");
					}else{
						StrList[15] = new String[3];
						StrList[15][0] = "�ʐM���s(" + errorcode + "," + rc + ")";
						StrList[15][1] = strdata[11].substring(5);
						StrList[15][2] = "";
						i = strdata[11].indexOf(' ');
						if(i > 0){
							StrList[15][2] = strdata[11].substring(i+1);
						}
						Bugln("Message(" + errorcode + "):" + strdata[11] + "\n");
					}
					stat2 |= 0x0001000;
				}
				if((mode & 0x00000002) != 0/*data[78] == 2*/ && (stat & 0x10000) == 0){
					stat2 &= ~0x8000000;	//�������ʕ\�����t���O����
				}
				if((stat & 0x0040000) != 0 && (stat & 0x0000100) == 0/* && (stat2 & 0x0010000) == 0*/){	//���X�\����(+���X�����擾��)�܂��A���X�g�{�b�N�X�͔�\��/*�u�b�N�}�[�N�\�����ł͂Ȃ�*/
					addCommand(command[0]);//���j���[
					addCommand(command[6]);//�߂�
				}
			}
		} catch(Exception e) {
//				strdata[7] = "�v���I�ȴװ";
			//strdata[7] = null;
			StrList[15] = new String[2];
			StrList[15][0] = "�v���I�ȴװ";
			StrList[15][1] = e.toString();
			//bagdata = bagdata + "\r\n" + e.toString();
			stat2 |= 0x0001000;
		}

		//if((stat & 0x0000004) != 0){stat ^= 0x0000004;}	//�L�[�X�g�b�v����
		//data[82] = data[82] / 10;
		///System.out.println("3�{��:" + data[82]);
		stat |= 0x1000000;	//��ʍX�V
		//data[16] = 0;
		stat3 &= ~0x0000810;	//��3������
		//stat3 &= ~0x0000010;	//���X���Ōォ��ǂރt���O�̍폜
		//stat3 &= ~0x0000800;	//�ʐM�@�\�g�p��
		stat &= ~0x10008014;		//���S������
		//stat &= ~0x0000004;	//�L�[�X�g�b�v����
		//stat &= ~0x0000010;	//�ʐM����
		//stat &= ~0x0008000;	//�X���ꗗ�̃����[�h����
		//stat &= ~0x10000000;	//�ʐM��̏���������
		//System.gc();	//�K�x�[�W�R���N�V����
	}

	//10+26+26
	//240�i��->10�i��
	/**
	 * 240�i��->10�i��
	 */
	public final int to10(byte b[]){
		if(b == null || b.length == 0)
			return 0;
		int i = b.length - 2;
		int j = 240;
		int ret = (b[i+1] & 0xFF) - 16;

		//System.out.println("i:" + (b[b.length - 1] & 0xFF) + "");
		while(i >= 0){
			//System.out.println("i:" + i + " N:" + ((b[i] & 0xFF) - 16) + " J:" + j);
			ret += ((b[i] & 0xFF) - 16) * j;
			i--;	j = j * 240;
		}


		return ret;
	}

	/**
	 * �L���b�V���N���A����
	 */
	final void clearcache(){
		int i;
		for(i = 0;i < CacheTitle.length;i++){
			//CacheResData[i] = null;
			//CacheBrdData[i] = null;
			//nCacheSt[i] = 0;
			//nCacheTo[i] = 0;
			nCacheTTL[i] = -1;
		}
		//�߂��������
		data[64] = 0;
		strdata[7] = "�ر����";
		stat2 |= 0x0001000;
	}

	/**
	 * �v�Z�A�����Ē~�ς����p�P�b�g��DL�����T�C�Y�◿���Ȃǂ�\��
	 */
	final void viewcost(){
		int i = data[14];

		//strdata[7] = null;
		StrList[15] = new String[8];
		StrList[15][0] = "DL��������";
		if(data[17] != 0){
			StrList[15][0] = StrList[15][0] + "[g]";
			i = data[17];
		}
		StrList[15][1] = i + "byte";
		StrList[15][2] = "�𓀌�";
		if(data[18] != 0){
			StrList[15][2] += "(" + ((data[18] - i) * 100 / data[18]) + "%��)";
		}
		StrList[15][3] = data[18] + "/+" + (data[18] - i) + "byte";
		if(data[69] != 0){
			StrList[15][4] = "�߹��(\\" + (data[69] / 1000) + ".";// + (data[69] % 1000) + "/p)";
			if(data[69] % 1000 < 100){StrList[15][4] += "0";}
			if(data[69] % 1000 < 10){StrList[15][4] += "0";}
			StrList[15][4] += data[69] % 1000 + "/p)";
		} else {

			StrList[15][4] = "�߹��(\\0.3/p)";
		}
		StrList[15][5] = "��" + data[46] / 100 + ".";
		if(data[46] % 100 < 10){StrList[15][5] += "0";}
		StrList[15][5] += data[46] % 100 + "�~";
		StrList[15][6] = "�߹�đ�̗݌v";
		StrList[15][7] = "��" + data[47] / 100 + ".";
		if(data[47] % 100 < 10){StrList[15][7] += "0";}
		StrList[15][7] += data[47] % 100 + "�~";
		stat2 |= 0x0001000;
	}


	/**
	 * �ݒ�����[�h���鏈��
	 */
	public final void Load(){
		//System.out.println("freemem " + (Runtime.getRuntime()).freeMemory());
		DataInputStream isp;
		int i, j;
		RecordStore recordStore;
		byte[] b = null;

		try {
			recordStore = RecordStore.openRecordStore("Setting", false);
			//b = recordStore.getRecord(1);
			isp = new DataInputStream(new ByteArrayInputStream(recordStore.getRecord(1)));
			recordStore.closeRecordStore();//��

			//ByteArrayOutputStream sv = new ByteArrayOutputStream();
			data[0] = isp.readByte() * 127 + isp.readByte();
			data[1] = isp.readByte() * 127 + isp.readByte();
			data[76] = isp.readByte();
			data[82] = isp.readByte() * 127 + isp.readByte();
			data[34] = isp.readByte();
			data[35] = isp.readByte();
			data[36] = isp.readByte();
			data[49] = isp.readByte() & 0xFF;
			data[47] = isp.readInt();
			data[55] = isp.readByte() * 127 + isp.readByte();
			data[56] = isp.readByte();
			i = isp.readByte() & 0xFF;
			if(i > 0){
				b = new byte[i];
				isp.read(b);
				server = new String(b);
			}
			data[57] = isp.readInt();
			i = isp.readByte() & 0xFF;
			if(i > 0){
				b = new byte[i];
				isp.read(b);
				extendedoption = new String(b);
			}
			//i = isp.readByte() & 0xFF;
			data[69] = isp.readInt();
			data[58] = isp.readByte() & 0xFF;	//�z�F�ݒ�
			data[80] = isp.readByte() & 0xFF;	//gzip���k��
			data[84] = isp.readInt();	//o 84 ��ǂ݋@�\�Ő�ǂ݂��s�����X��
			if(data[84] > 1000){data[84] = 1000;}

			data[87] = isp.readInt();	//o 87 ���l�̐ݒ�
			if((data[87] & 0xFF) == 0){if((data[57] & 0x00000002) != 0){data[87] = 1;} else {data[87] = 2;}}
			if((data[87] & 0xFF00) == 0){if((data[57] & 0x00000004) != 0){data[87] |= 0x0100;} else {data[87] |= 0x0200;}}
			data[90] = isp.readInt();	//o 90 ���܂łɓǂ񂾃��X�̐�
			name = isp.readUTF();
			mail = isp.readUTF();
			data[91] = isp.readInt();	//o 91 �p�P�b�g��x��
			data[92] = isp.readInt();	//o 92 �X���b�h�ꗗ�ł̕\�����@
			data[94] = isp.readInt();	//o 94 7�L�[�̋@�\
			data[96] = isp.readInt();	//o 96 �������݉�ʂ�URL����׳�ސݒ�
			data[97] = isp.readInt();	//o 97 �d��&�d�gϰ��̕\��
			data[98] = isp.readInt();	//o 98 ��������
			data[99] = isp.readInt();	//o 99 ����̪��
			//������ݸ
			i = isp.readByte() & 0xFF;
			if(i > 0){
				b = new byte[i];
				isp.read(b);
				cushionlink = new String(b);
			}
			data[71] = isp.readInt();	//o 71 7���̋@�\ for �޸�
			data[73] = isp.readInt();	//o 71 7���̋@�\ for ڽ
			data[74] = isp.readInt();	//o 71 0���̋@�\
			//NG���[�h
			/*i = isp.readByte() & 0xFF;
			if(i > 0){
				b = new byte[i];
				isp.read(b);
				ngword = new String(b);
			}*/
			isp.close();


		} catch(Exception e){
			//System.out.println("Default");

			//�f�t�H���g�ݒ�(����N����)//////
			data[0] = 10;	//�X���̈�x�ɓǂݍ��ސ�
			data[1] = 15;	//���X�̈�x�ɓǂݍ��ސ�
			data[76] = 1;	//���k��
			data[49] = 0x1D;//0x01 | 0x04 | 0x08 | 0x10
			//stat |= 0x0000200;
			//�t�H���g
			font = Font.getDefaultFont();
			if(font.getHeight() > width / 9/*13*/){data[36] = 0;} else {data[36] = 1;}	//�t�H���g���傫���Ȃ珬��������
			data[34] = 1;	//�s��
			data[35] = 5;	//�X�N���[����
			data[82] = 50;	//�X���b�h�X�s�[�h
			data[47] = 0;	//�p�P�b�g��
			data[80] = 6;	//gzip���k��
			data[87] = 0x101;
//			data[57] = 0x00004001;
//			data[57] = 0x0000C001;
			data[57] = 0x00024201;

			//�ݒ肱���܂�/////////////////////
		}
		if( (data[57] & 0x00000001) == 0 ){
			data[57] = 0x00000001;
		}
		if(server.indexOf("http") != 0){
			//�f�t�H���g�ݒ�//////////////////
			server = server_url[0];
/*
			if((System.currentTimeMillis() / 100) % 2 == 0){
				server = "http://imona.net/";
			} else {
				server = "http://imona.zive.net/";
			}
*/
		}

		if(server.substring(server.length() - 1).equals("/") == false){
			server = server + "/";
		}
		//�f�t�H���g�ݒ�//////////////////
		if(data[56] == 0){data[56] = 2;}	//�ڏ��̕\��		�ڈꗗ���ƭ���
		//�u�b�N�}�[�N
		try {
			int nbookmark = 0;	//�u�b�N�}�[�N�̐��𐔂��邽�߂Ɏg�p�B

			recordStore = RecordStore.openRecordStore("Bookmark", false);
			b = recordStore.getRecord(1);
			data[40] = b[0] * 127 + b[1];
			data[66] = b[2] * 127 + b[3];

			if(data[40] < 200){
				data[66] = data[40];
				//nbookmark = 0;
				data[40] = 200;
			}
			BookMark = new String[data[40]];
			BookMarkData = new int[data[40]*3];
			data[66] = recordStore.getNumRecords()-1;
			//for(i = 0;i < data[40];i++){
			for(i = 0;i < data[66];i++){
				b = recordStore.getRecord(i + 2);
				StrList[0] = split( new String(b), '\t');


				BookMark[i] = StrList[0][0];
				//tBookMark[i] = trimstr(BookMark[i], width - 29);
				BookMarkData[i*3] = Integer.parseInt(StrList[0][1],36);	//brd
				BookMarkData[i*3+1] = Integer.parseInt(StrList[0][2],36);	//th
				BookMarkData[i*3+2] = Integer.parseInt(StrList[0][3],36);	//res
				if(/*nbookmark >= 0 && */!(BookMark[i].length() == 0)){
					nbookmark = i + 1;
				}
			}

			for(i = data[66];i < data[40];i++){
				BookMark[i] = "";
			}
			//if(nbookmark >= 0){
			data[66] = nbookmark;
			//}

		} catch(Exception e){
			//System.out.println("New BookMark");

			data[40] = 200;	//�u�b�N�}�[�N�̐�
			BookMark = new String[data[40]];
			BookMarkData = new int[data[40]*3];
			for(i = 0;i < data[40];i++){
				BookMark[i] = "";
			}
			//�f�t�H���g�̃u�b�N�}�[�N
			BookMark[0] = "���g�їp2ch��׳�� iMona��";
			BookMark[1] = "[��]iMona�޲��";
			BookMark[2] = "[��]2ch�����ē�";
			BookMark[3] = "[��]۸ޑq��";
			BookMarkData[0] = 1108;
			BookMarkData[1] = 1078396443;
			BookMarkData[2] = 9999;
			BookMarkData[3] = 3507;
			//BookMarkData[4] = 0;
			//BookMarkData[5] = 0;
			BookMarkData[6] = 3508;
			//BookMarkData[7] = 0;
			//BookMarkData[8] = 0;
			BookMarkData[9] = 3506;
			//BookMarkData[10] = 0;
			//BookMarkData[11] = 0;
			//tBookMark[0] = trimstr(BookMark[0], width - 29);
			//tBookMark[1] = trimstr(BookMark[1], width - 29);
			//tBookMark[2] = trimstr(BookMark[2], width - 29);
			//tBookMark[3] = trimstr(BookMark[3], width - 29);
			data[66] = 4;
//			e.printStackTrace();
		}
		SetFont();




		//�ꗗ
		try {
			recordStore = RecordStore.openRecordStore("Brdlist", false);
			brdarray = recordStore.getRecord(1);
			recordStore.closeRecordStore();//��
		} catch(Exception e){
			brdarray = null;
		}
		//brdinit();
		Thread initth = new Thread() {
			public final void run() {
				brdinit();
			}
		};
		initth.start();
		//�������̂��߂Ƀg���~���O�ς݂̃u�b�N�}�[�N�������炩���ߍ쐬
		//for(i = 0;i < BookMark.length;i++){
		//	tBookMark[i] = trimstr(BookMark[i], width - 29);
		//	if(!(tBookMark[i].equals(""))){
		//		data[66] = i + 1;
		//	}
		//}
		Bugln("SettingLoad...Ok!\n");
	}

	/**
	 * �u�b�N�}�[�N�̕ۑ�
	 * @param n
	 */
	public final synchronized void SaveBookMark(int n){
		int /*i = 0, */first = 0;
		byte b[] = new byte[1];
/*
		try {
//			RecordStore.deleteRecordStore("Bookmark");
		} catch(Exception e){
		}
*/
		try {
			String str;

			System.gc();	//�K�x�[�W�R���N�V����
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			RecordStore recordStore = RecordStore.openRecordStore("Bookmark", true);
			if(recordStore.getNumRecords() == 0){first = 1;}
			out.write(data[40] / 127);	//o 40 �ޯ�ϰ��̐�
			out.write(data[40] % 127);
			out.write(data[66] / 127);	//o 66 �ޯ�ϰ��̎g�p��(�Ԃ̋󔒂��܂�)
			out.write(data[66] % 127);
			//if(first == 1){
			if(recordStore.getNumRecords() == 0){
				recordStore.addRecord(out.toByteArray(), 0, out.toByteArray().length);
			} else {
				recordStore.setRecord( 1, out.toByteArray(), 0, out.toByteArray().length);
			}
			out.close();
			out = null;


			if(first == 1 || n == -1){
				for(n = 0;n < data[40];n++){
					str = BookMark[n] + "\t" + Integer.toString(BookMarkData[n * 3],36) + "\t" + Integer.toString(BookMarkData[n * 3 + 1],36) + "\t" + Integer.toString(BookMarkData[n * 3 + 2],36);
					//if(first == 1){
					if(recordStore.getNextRecordID() <= 2 + n){
						recordStore.addRecord(str.getBytes(), 0, str.getBytes().length);
					} else/* if(BookMarkData[i * 3 + 1] != 0 || !(BookMark[i].equals("")))*/{
						recordStore.setRecord( 2 + n,str.getBytes(), 0, str.getBytes().length);
					}
				}
			} else {
				str = BookMark[n] + "\t" + Integer.toString(BookMarkData[n * 3],36) + "\t" + Integer.toString(BookMarkData[n * 3 + 1],36) + "\t" + Integer.toString(BookMarkData[n * 3 + 2],36);
//				recordStore.setRecord( 2 + n,str.getBytes(), 0, str.getBytes().length);
				if(recordStore.getNextRecordID() <= 2 + n){
					recordStore.addRecord(str.getBytes(), 0, str.getBytes().length);
				} else {
					recordStore.setRecord( 2 + n,str.getBytes(), 0, str.getBytes().length);
				}

			}
			recordStore.closeRecordStore();//��
		} catch(Exception e){
			strdata[7] = "�װ����" + e.toString();
			stat2 |= 0x0001000;
		}
	}

	/**
	 * �ݒ�̕ۑ�
	 */
	public final synchronized void saveSetting(){
		byte b[];// = new byte[1];
		try {
			System.gc();	//�K�x�[�W�R���N�V����

			ByteArrayOutputStream out2 = new ByteArrayOutputStream(500);
			DataOutputStream out = new DataOutputStream( out2 );
			//DataOutputStream out = new DataOutputStream( new ByteArrayOutputStream(500) );

			out.write(data[0] / 127);	//o  0 �X���̈�x�ɓǂݍ��ސ�
			out.write(data[0] % 127);
			out.write(data[1] / 127);	//o  1 ���X�̈�x�ɓǂݍ��ސ�
			out.write(data[1] % 127);
			out.write(data[76]);		//o  2 ���k��
			out.write(data[82] / 127);	//o 17 �X���b�h�̃X�s�[�h
			out.write(data[82] % 127);
			out.write(data[34]);		//o 34 �s��
			out.write(data[35]);		//o 35 �P�X�N���[���ňړ������
			out.write(data[36]);		//o 36 �����̃T�C�Y(0:�� 1:�� 2:��)
			out.write(data[49]);		//o 49 ���̑��ݒ�(�I�[�g�X�N���[���E�b�\��)

			//WriteInt(out, data[47]);	//o 47 �p�P�b�g��(���v)
			out.writeInt(data[47]);
			out.write(data[55] / 127);	//o 55 �ŐVڽ�œǂސ�
			out.write(data[55] % 127);
			out.write(data[56]);		//o 56 �ڏ��̕\��		�ڈꗗ���ƭ���

			if(server.substring(server.length() - 1).equals("/") == false){
				server = server + "/";
			}
			b = server.getBytes();
			out.write(b.length);	//���ް�ݒ�
			out.write(b, 0, b.length);
			//WriteInt(out, data[57]);	//o 57 �ۑ�����t���O2
			out.writeInt(data[57]);		//o 57 �ۑ�����t���O2
			if(extendedoption.length() == 0){
				out.write(0x00);
			} else {
				b = extendedoption.getBytes();
				out.write(b.length);	//�g����߼��
				out.write(b, 0, b.length);
			}
			out.writeInt(data[69]);		//o 69 �p�P�b�g��̒P��(\/1000packet \0.3/packet->\300/1000packet) 0�̏ꍇ�͎���
			out.write(data[58]);		//o 58 �z�F�ݒ�
			out.write(data[80]);		//o 80 gzip�̈��k��
			out.writeInt(data[84]);		//o 84 ��ǂ݋@�\�Ő�ǂ݂��s�����X��
			out.writeInt(data[87]);		//o 87 ���l�̐ݒ�
			out.writeInt(data[90]);		//o 90 ���܂łɓǂ񂾃��X�̐�
			if(name.getBytes().length == 0){
				out.writeShort(0);
			} else {
				//b = name.getBytes();
				//out.writeShort(b.length);		//�������ݎ��̖��O
				//out.write(b, 0, b.length);
				out.writeUTF(name);
			}
			if(mail.getBytes().length == 0){
				out.writeShort(0);
			} else {
				//b = mail.getBytes();
				//out.writeShort(b.length);		//�������ݎ��̃��A�h
				//out.write(b, 0, b.length);
				out.writeUTF(mail);
			}
			//out.writeUTF(name);
			//out.writeUTF(mail);
			out.writeInt(data[91]);		//o 91 �p�P�b�g��x��
			out.writeInt(data[92]);		//o 92 �X���b�h�ꗗ�ł̕\�����@
			out.writeInt(data[94]);		//o 94 7�L�[�̋@�\
			out.writeInt(data[96]);		//o 95 ������ʂ�URL
			out.writeInt(data[97]);		//o 97 �d�r&�d�g�}�[�N�̕\������
			out.writeInt(data[98]);		//o 98 ��������
			out.writeInt(data[99]);		//o 99 ����̪��
			//������ݸ
			if(cushionlink.length() == 0){
				out.write(0x00);
			} else {
				b = cushionlink.getBytes();
				out.write(b.length);	//������ݸ
				out.write(b, 0, b.length);
			}
			out.writeInt(data[71]);		//o 71 7���̋@�\�@for �޸�
			out.writeInt(data[73]);		//o 73 7���̋@�\�@for ��
			out.writeInt(data[74]);		//o 74 0���̋@�\
			/*if(ngword.equals("")){
				out.write(0x00);
			} else {
				b = ngword.getBytes();
				out.write(b.length);	//������ݸ
				out.write(b, 0, b.length);
			}*/
			out.writeInt(0);
			out.writeInt(0);
			out.writeInt(0);

			RecordStore recordStore = RecordStore.openRecordStore("Setting", true);

			//��������
			b = out2.toByteArray();
			if(recordStore.getNumRecords() == 0){
				recordStore.addRecord(b, 0, b.length);
				//recordStore.addRecord(out2.toByteArray(), 0, out2.toByteArray().length);
			} else {
				recordStore.setRecord( 1, b, 0, b.length);
				//recordStore.setRecord( 1, out2.toByteArray(), 0, out2.toByteArray().length);
			}
			recordStore.closeRecordStore();//��
			out2.reset();//������

			//SaveBookMark(-1);
		} catch(Exception e){
			strdata[7] = e.toString();
			stat2 |= 0x0001000;
		}
	}

	/**
	 * ������𕪊�
	 * @param str �Ώە�����
	 * @param ch
	 * @return String
	 */
	public final String[] split( final String str, int ch){
		Vector vecStr = new Vector();
		int end, start = 0;
		while(true){
			end = str.indexOf(ch , start);
			if(end == -1){
				vecStr.addElement(str.substring(start));
				break;
			} else {// ���߂�ꂽ�������������Ƃ�
				vecStr.addElement(str.substring(start, end));
				start = end + 1;
			}
		}
		String[] result = new String[vecStr.size()];
		vecStr.copyInto(result);
		return result;
	}

	/**
	 * ���C�����j���[
	 * @param i �I�������ӏ�
	 */
	public final void mainmenu(int i){
		switch(i){
			case 0:	//�ꗗ
				//�J�e�S�����X�g�ǂݍ��ݍς݃t���O�����܂ő҂�
				while((stat & 0x0000800) == 0){
					try{Thread.sleep(10);}catch (Exception e){}
				}

				ListBox = StrList[12];
				stat |= 0x2000;	//�J�e�S���I��
			break;
			case 1:	//�ޯ�ϰ�
				data[67] = 0; data[68] = 0;
				showBookMark(0);
			break;
			case 2:	//URL�w��
				stat2 &= ~0x0004000;	//function����

				tfield = new LocalizedTextField("�ڥ��URL","",300,LocalizedTextField.ANY);
				inputForm = new Form("URL�w��");
				inputForm.append(tfield);

				inputForm.addCommand(command[8]);
				inputForm.addCommand(command[2]);

				inputForm.setCommandListener(this);

				disp.setCurrent(inputForm);

				stat3 |= 0x0001000;
			break;
			case 3:	//�ݒ�
				ListBox = StrList[4];
				stat2 |= 0x0000001;	//�ݒ�
			break;
			//case 3:	//�f�[�^�t�H���_
				//-/DataFolder = GetFileList(0);
				//-/ListBox = GetNameList(DataFolder);
			//-/stat |= 0x0000200;	//�f�[�^�t�H���_�[
			//break;
			case 4:	//�I��
				stat |= 0x0004;	//�L�[���b�N
				//SaveSetting();
				parent.destroyApp(false);
				parent.notifyDestroyed();
			break;
		}
	}

	/**
	 * �X���b�h���j���[
	 * @param i �I�������ӏ�
	 */
	public final void threadmenu(int i){
		switch(i){
			case 0:	//�ꗗ�ɖ߂� ���ǂ�
				backfromfirst();
			break;
			case 1:	//�ŐVڽ���݂�
				httpinit(2,nCacheBrd[nCacheIndex]/*data[3]*/,-1,data[55],nCacheTh[nCacheIndex]/*nThread*/[data[60]]);
				stat |= 0x40000000;	//Loading���̑���͕s�\
				break;
				case 2:	//ڽ�ԍ����w��
				if((stat & 0x0000400) != 0){	//�ݒ肵����
					//setResnum(nCacheBrd[nCacheIndex]/*data[3]*/, nCacheTh[nCacheIndex]/*nThread*/[data[4]]/*data[2]*/);
					setResnum(nCacheBrd[nCacheIndex]/*data[3]*/, nCacheTh[nCacheIndex]/*nThread*/[data[60]]/*data[2]*/);
				} else {
					setResnum(nCacheBrd[nCacheIndex], 1);
				}
			break;
			case 3:	//1�̂ݎ擾
				httpinit(2,nCacheBrd[nCacheIndex]/*data[3]*/,1,1,nCacheTh[nCacheIndex]/*nThread*/[data[60]]);
			break;
			case 4:	//�ޯ�ϰ��ɓo�^
				//if(-1 == EditBookMark( 0, CacheBrdData[nCacheIndex]/*ThreadName*/[data[4]], nCacheBrd[nCacheIndex]/*data[3]*/, nCacheTh[nCacheIndex]/*nThread*//*nThread*/[data[4]], 0)){
				if(-1 == EditBookMark( 0, CacheBrdData[nCacheIndex]/*ThreadName*/[data[60]], nCacheBrd[nCacheIndex]/*data[3]*/, nCacheTh[nCacheIndex]/*nThread*//*nThread*/[data[60]], 0)){
					//strdata[7] = null;
					//StrList[15] = new String[3];
					//StrList[15][0] = "= �o�^���s =";
					//StrList[15][1] = "�ޯ�ϰ�������";
					//StrList[15][2] = "�ς��ł��B";
					strdata[7] = "�o�^���s";
					stat2 |= 0x0001000;
				}
			break;
			case 5:	//�گ�ނ𗧂Ă�
				//if(strdata[1] != null && data[79] == nCacheBrd[nCacheIndex]/*data[3]*/){
				openbrowser(server + "2.cgi?v=C&m=w2&b=" + nCacheBrd[nCacheIndex]/*data[3]*/ , 1);
				/*} else {
					httpinit(6, nCacheBrd[nCacheIndex], 0, 0, 0);//dlbrdlist();
				}*/
				//break;
			case 6:	//�ēǍ�
				if((stat2 & 0x8000000) == 0) {
				//	stat ^= 0x10000;	//�ڑI�𒆉���
					//stat |= 0x8000;	//�X���ꗗ�c�k��
					strdata[8] = strdata[9];	//���݂̃^�C�g�����g�p����

					stat |= 0x0008000;	//�X���ꗗ�̃����[�h
	//				i = data[5] - data[4] + 1;
					//data[5] = i - 1;
	//				httpinit(1,nCacheBrd[nCacheIndex]/*data[3]*/,i,i + data[0] - 1,0);
					httpinit(1,nCacheBrd[nCacheIndex]/*data[3]*/,nCacheSt[nCacheIndex],nCacheSt[nCacheIndex] + data[0] - 1,0);
				}
			break;
			case 7:	//������ر
				clearcache();
			break;
			case 8:	//�ʐM�̏ڍ�
				viewcost();
			break;

			case 9:	//�ƭ������
				//backfromthreadlist();
				stat &= ~0x0000100;	//�ƭ�����
				addCommand(command[0]);
				addCommand(command[6]);
			break;
		}
	}

	/**
	 * ���X���j���[
	 * @param i �I�������ӏ�
	 * @param j �����A���݂̃��X��
	 */
	public final void resmenu(int i, int j){
		switch(i){
			case 0:	//�ڈꗗ�ɖ߂�
				backfromfirst();
			break;
			case 1:	//�ŐVڽ
				httpinit(2,nCacheBrd[nCacheIndex]/*data[3]*/,-1,data[55],nCacheTh[nCacheIndex][0]/*data[2]*//*nThread[data[4]]*/);
				stat |= 0x40000000;	//Loading���̑���͕s�\
				//stat &= ~0x0040000;	//���j���[��\�������邽�߂Ɉꎞ�I�Ƀ��X�\����������
				//viewres();
				addCommand(command[0]);//���j���[
				addCommand(command[6]);//�߂�
				stat &= ~0x0000100;	//���X�g�{�b�N�X����
			break;

			case 2:	//ڽ�ԍ��@�w��ʒu�Ɉړ��@ڽ�ԍ����w��@ڽ�Ԏw��
				if((stat & 0x0000400) != 0){	//�ݒ肵����
					setResnum(nCacheBrd[nCacheIndex]/*data[3]*/, nCacheTh[nCacheIndex][0]/*data[2]*/);
					//stat &= ~0x0040000;	//���j���[��\�������邽�߂Ɉꎞ�I�Ƀ��X�\����������
					//viewres();
					addCommand(command[0]);//���j���[
					addCommand(command[6]);//�߂�
					stat &= ~0x0000100;	//���X�g�{�b�N�X����
				} else {
					setResnum(nCacheBrd[nCacheIndex], data[6] + nCacheSt[nCacheIndex]/*data[7]*/);
				}
			break;

			case 3:	//�������݁@����&�I��
				if((data[57] & 0x00080000) != 0){if(bodytext.indexOf(">>") <= 0 & bodytext.length() >= 5){bodytext = ">>" + Integer.toString(data[6] + nCacheSt[nCacheIndex]) + "\n";}}
				if(strdata[1] != null && data[79] == nCacheBrd[nCacheIndex]/*data[3]*/){
					viewwritepanel();
				} else {
					stat4 |= 0x0040000;
					httpinit(6, nCacheBrd[nCacheIndex]/*data[3]*/, 0, 0, 0);//dlbrdlist();
				}
			break;
			//case 4:	//�����Ƀ��X
			//	bodytext = bodytext + ">>" + Integer.toString(data[6] + nCacheSt[nCacheIndex]) + "\n";
			//	if(strdata[1] != null && data[79] == nCacheBrd[nCacheIndex]/*data[3]*/){
			//		viewwritepanel();
			//	} else {
			//		stat4 |= 0x0040000;
			//		httpinit(6, nCacheBrd[nCacheIndex]/*data[3]*/, 0, 0, 0);//dlbrdlist();
			//	}
			//break;
			case 4:	//������ʂ�URL
				stat2 &= ~0x0004000;	//function����

				try{
				String buf = server + "2.cgi?v=C&m=w&b=" + nCacheBrd[nCacheIndex]/*data[3]*/ + "&t=" + nCacheTh[nCacheIndex][0]/*data[2]*//*nThread[data[4]]*/;
				if(data[96] == 1){
					//System.out.println(buf);
					openbrowser(buf, 1);
				} else {
					tbox = new LocalizedTextBox(StrList[3][i], buf, buf.length(), LocalizedTextField.ANY);
					tbox.addCommand(command[2]);
					stat3 |= 0x0004000;
					tbox.setCommandListener(this);
					disp.setCurrent(tbox);
				}
				} catch(Exception e){
					stat3 &= ~0x0004000;
					disp.setCurrent(this);
				}
			break;
			case 5:	//�ޯ�ϰ�
				showBookMark(0);
			break;
			case 6:	//�ޯ�ϰ��ɓo�^
				if(-1 == EditBookMark( 0, CacheTitle[nCacheIndex]/*strdata[9]*//*ThreadName[data[4]]*/, nCacheBrd[nCacheIndex]/*data[3]*/, nCacheTh[nCacheIndex][0]/*data[2]*//*nThread[data[4]]*/, data[6]+nCacheSt[nCacheIndex]/*data[7]*/)){
					//strdata[7] = null;
					//StrList[15] = new String[3];
					//StrList[15][0] = "= �o�^���s =";
					//StrList[15][1] = "�ޯ�ϰ�������";
					//StrList[15][2] = "�ς��ł��B";
					strdata[7] = "�o�^���s";
					stat2 |= 0x0001000;
				}
			break;

			case 7:	//�ݒ�
				ListBox = StrList[4];
				stat2 |= 0x0000001;	//�ݒ�
				//data[19] = ListBox.length;
				data[10] = 0;data[11] = 0;
			break;
			case 8:	//÷���ޯ��
				stat2 &= ~0x0004000;	//function����

				try{
					/*String buf = "";
					for(i = 0; i < data[85]; i++){
						buf = buf + CacheResData[nCacheIndex][i][0]  + "\n";
					}
					tbox = new LocalizedTextBox("ڽ", buf, buf.length(), LocalizedTextField.ANY);*/
					//System.out.println("\r\n\r\n-------" + resstr + "------\r\n\r\n");
					String r_resdata = new String(resdata);
					r_resdata = r_resdata.replace('\t','\r');
					r_resdata = Integer.toString(data[6] + nCacheSt[nCacheIndex]) + ":" + strdata[3] + "\r" + r_resdata;
					tbox = new LocalizedTextBox("ڽ", r_resdata, r_resdata.length(), LocalizedTextField.ANY);
					//inputForm = new Form(StrList[3][i]);
					//inputForm.addCommand(command[4]);
					tbox.addCommand(command[2]);
					stat3 |= 0x0002000;
					//inputForm.append(tfield);
					//inputForm.append(new TextBox("ڽ", buf, buf.length(), TextField.ANY));
					tbox.setCommandListener(this);
					disp.setCurrent(tbox);
				} catch(Exception e){
					stat3 &= ~0x0002000;
					System.gc();
					disp.setCurrent(this);
				}
				stat |= 0x1000000;	//��ʍX�V
			break;
			case 9:	//÷���ޯ��(URL)
				stat2 &= ~0x0004000;	//function����
				try{
					if(data[60] != -1){
						String buf = "";

						i = Linklist2[data[60]] % 1000;
						j = Linklist[data[60]] / 1000;
						if(j % 10000 != 0){i += iDivStr[j % 10000-1];}
						buf = resstr.substring(i, i + (j / 10000));
						tbox = new LocalizedTextBox("URL", buf, buf.length(), LocalizedTextField.ANY);
						tbox.addCommand(command[2]);
						stat3 |= 0x0002000;
						tbox.setCommandListener(this);
						disp.setCurrent(tbox);
					}
				} catch(Exception e){
					stat3 &= ~0x0002000;
					System.gc();
					disp.setCurrent(this);
				}
			break;

			case 10:	//������ر
				clearcache();
			break;
			case 11:	//AAS
				if(strdata[1] != null && data[79] == nCacheBrd[nCacheIndex]/*data[3]*/){
					//data[78] |= 0x0000400; //AAS
					strdata[0] = strdata[1];
					if(strdata[0].charAt(strdata[0].length() - 1) == '/'){
						strdata[0] = strdata[0].substring( 0, strdata[0].length() - 1);
					}
					int it = strdata[0].lastIndexOf( '/', strdata[0].length() - 1);
					bbsname = strdata[0].substring(it + 1);
					strdata[0] = strdata[0].substring( 0, it);
					String buf = "example.ddo.jp/aas/a.i/" + strdata[0] + "/" + bbsname + "/" + nCacheTh[nCacheIndex][0] + "/" + (data[6] + nCacheSt[nCacheIndex]);

					openbrowser(buf,1);
				}else{
					httpinit(6, nCacheBrd[nCacheIndex]/*data[3]*/, 0, 0, 0);//dlbrdlist();
				}

			break;
			/*case 12:   //�گ�ނ̏ڍ�
				StrList[15] = new String[2];
				StrList[15][0] = "�گ�ނ̏ڍ�";
				//StrList[15][1] = e.toString();
				stat2 |= 0x0001000;
			break;*/
			/*case 12://c.2ch�ł̱���
				if(strdata[1] == null && data[79] != nCacheBrd[nCacheIndex]/*data[3]){
					data[78] |= 0x0000800; //c.2ch.net
					httpinit(6, nCacheBrd[nCacheIndex]/*data[3], 0, 0, 0);//dlbrdlist();
				}else{
					strdata[0] = strdata[1];
					if(strdata[0].charAt(strdata[0].length() - 1) == '/'){
						strdata[0] = strdata[0].substring( 0, strdata[0].length() - 1);
					}
					int it = strdata[0].lastIndexOf( '/', strdata[0].length() - 1);
					bbsname = strdata[0].substring(it + 1);
					//strdata[0] = strdata[0].substring( 0, it);
					String buf = "c.2ch.net/test/-/" + bbsname + "/" + nCacheTh[nCacheIndex][0] + "/" + (data[6] + nCacheSt[nCacheIndex]) + "-";

					openbrowser(buf,1);
				}
			break;*/
			case 12:	//�ʐM�̏ڍ�
				viewcost();
			break;
			case 13:	//�ƭ������
				stat ^= 0x0100;
				addCommand(command[0]);
				addCommand(command[6]);
			break;
		}
	}

	/**
	 * �u�b�N�}�[�N���j���[(���X)
	 * @param i �I�������ӏ�
	 * @param j�@�����u�b�N�}�[�N�̑I�����Ă���ӏ�
	 */
	public final void bookmarkmenu(int i, int j){ //�ޯ�ϰ��ƭ�(ڽ)
		int k = 0;
		if(i == 0){//�ŐVڽ��ǂ�
			httpinit(2,BookMarkData[j * 3],-1,data[55],BookMarkData[j * 3 + 1]);
			thttpget();
		} else if(i == 1){//ڽ�Ԏw��
			if(0 < BookMarkData[j * 3 + 2] && BookMarkData[j * 3 + 2] < 9999 ){
				setResnum(BookMarkData[j * 3], BookMarkData[j * 3 + 2]);
			} else {
				setResnum(BookMarkData[j * 3], 1);
			}
			/*if((stat4 & 0x0000800) != 0){
				addCommand(command[2]);
			}*/
		} else if(i == 2){//����&�I��
//			openbrowser(server.substring(7) + "2.cgi?v=C&m=w&b=" + BookMarkData[j * 3] + "&t=" + BookMarkData[j * 3 + 1]);
			if(strdata[1] != null && data[79] == BookMarkData[j * 3]){
				viewwritepanel();
			} else {
				stat4 |= 0x0040000;
				httpinit(6, BookMarkData[j * 3], 0, 0, 0);//dlbrdlist();
			}
		} else if(i == 3){//������ʂ�URL
			stat2 &= ~0x0004000;	//function����

			try{
				String buf = server + "2.cgi?v=C&m=w&b=" + BookMarkData[j * 3] + "&t=" + BookMarkData[j * 3 + 1];
				if(data[96] == 1){
					//System.out.println(buf);
					openbrowser(buf, 1);
					//BrowserConnection conn = (BrowserConnection)Connector.open( "url://" + buf);
					//conn.connect();
					//conn.close();
					//stat |= 0x1000000;	//��ʍX�V
				}else{
					tbox = new LocalizedTextBox("������ʂ�URL", buf, buf.length(), LocalizedTextField.ANY);
					tbox.addCommand(command[2]);
					stat3 |= 0x0004000;
					tbox.setCommandListener(this);
					disp.setCurrent(tbox);
				}
			} catch(Exception e){
				stat3 &= ~0x0004000;
				disp.setCurrent(this);
			}
		} else if(i == 4){//����
			commandAction(command[7]);
		} else if(i == 5){	//�ҏW
			commandAction(command[3]);
		} else if(i == 6){//�ޯ�ϰ��̏���
			EditBookMark( - j - 1, "", 0, 0, 0);	//����
		} else if(i == 7){//���Ԃ��l�߂�
			int n = data[66];
			for(j = 0; j < data[66]; j++){
				if(BookMarkData[j * 3 + 1] == 0 && BookMark[j].length() == 0){
					for(k = j+1; k < data[66]; k++){
						if(!(BookMarkData[k * 3 + 1] == 0 && BookMark[k].equals(""))){
							ChangeBookMark(j, k);	n = j + 1;
							break;
						}
					}
				}
			}
			data[66] = n;
		} else if(i == 8 && data[66] < data[40]){//���Ԃ����
			for(k = data[66]; k > j; k--){
				ChangeBookMark(k-1, k);
			}
			data[66]++;
		} else if(i == 9){//����߰�		���/���߰�
			try{
				String buf = "iB", buf2;
				if((stat2 & 0x0004000) != 0){	//function
					if(BookMarkData[j * 3 + 1] == 0 && BookMark[j].length() == 0){j++;}
					k = j + 10;
					if(k > data[66]){k = data[66];}
				} else {
					j = 0;	k = data[66];
				}

				for(; j < k; j++){
					buf2 = BookMark[j];
					while(true){
						i = buf2.indexOf(",");	if(i < 0){break;}
						buf2 = buf2.substring(0,i) + "%2C" + buf2.substring(i+2);
					}
					buf += "," + buf2 + "," + Integer.toString(BookMarkData[j * 3],36) + "," + Integer.toString(BookMarkData[j * 3 + 1],36) + "," + Integer.toString(BookMarkData[j * 3 + 2],36);
				}

				tbox = new LocalizedTextBox("����߰�", buf, buf.length(), LocalizedTextField.ANY);
				tbox.addCommand(command[2]);
				stat3 |= 0x2000000;
				tbox.setCommandListener(this);
				disp.setCurrent(tbox);
			} catch(Exception e){
				stat3 &= ~0x2000000;
				disp.setCurrent(this);
			}
		} else if(i == 10){//���߰�
			try{
				tbox = new LocalizedTextBox("���߰�", null, 5000, LocalizedTextField.ANY);
				tbox.addCommand(command[8]);
				tbox.addCommand(command[2]);
				stat3 |= 0x4000000;
				tbox.setCommandListener(this);
				disp.setCurrent(tbox);
			} catch(Exception e){
				stat3 &= ~0x4000000;
				disp.setCurrent(this);
			}
		}/* else if(i == 11){//�S����
			try{
				tbox = new LocalizedTextBox("���߰�", null, 5000, LocalizedTextField.ANY);
				tbox.addCommand(command[8]);
				tbox.addCommand(command[2]);
				stat3 |= 0x4000000;
				tbox.setCommandListener(this);
				disp.setCurrent(tbox);
			} catch(Exception e){
				stat3 &= ~0x4000000;
				disp.setCurrent(this);
			}
		}*/
	}

	/**
	 * �ݒ�g�b�v���j���[
	 * @param i �I�������ӏ�
	 */
	public final void settingmenu(int i){
		switch(i){
			case 0:	//��ؽĂ̍X�V,��ؽčX�V
				//Load();

				httpinit(0,0,0,0,0);//dlbrdlist();

			break;
			case 1:	//�ݒ�̕ۑ�
				saveSetting();
				strdata[7] = "�ۑ�����";//StrList[10][11] + StrList[10][19];
				stat2 |= 0x0001000;
			break;
			case 2:	//�\���ݒ�
				ListBox = StrList[5];
				//stat2 ^= 0x0000001;	//�ݒ�
				stat2 |= 0x0000002;	//�\���ݒ�
			break;
			case 3:	//����ݒ�
				ListBox = StrList[6];
				//stat2 ^= 0x0000001;	//�ݒ�
				stat2 |= 0x0000004;	//����ݒ�
			break;
/*
			case 3:	//�F�̐ݒ�
			//	ListBox = ColSetMenu;
				//stat2 ^= 0x0000001;	//�ݒ�
			//	stat2 |= 0x0000004;	//�F�̐ݒ�
			break;
*/
			case 4:	//�ʐM�ݒ�
				ListBox = StrList[7];
				//stat2 ^= 0x0000001;	//�ݒ�
				stat2 |= 0x0000008;	//�ʐM�ݒ�
			break;

			case 5:	//���̑�
				ListBox = StrList[8];
				stat2 |= 0x0000020;	//���̑�
			break;
			case 6:	//Ҳ��ƭ��ɖ߂�
				if((stat & 0x40000) != 0){	//���X�\����
					ListBox = StrList[3];
				} else {
					ListBox = StrList[1];
				}
				stat2 ^= 0x0000001;	//�ݒ����
			break;
		}
	}

	/**
	 * �\���ݒ胁�j���[
	 * @param i �I�������ӏ�
	 */
	public final void viewmenu(int i){

		switch(i){
		case 0:	//��������
			if((stat & 0x0000400) != 0){	//�ݒ肵����
				data[36] = data[28];
				stat ^= 0x0000400;
				stat2 ^= 0x0000200;	//�P����
				SetFont();
				if((stat & 0x40000) != 0){	//���X�\����
					makeRes(/*0*/);
				}
			} else {
				stat |= 0x0000400;
				stat2 |= 0x0000200;	//�P����
				data[26] = 2;	//��
				data[27] = 0;	//��
				data[28] = data[36];	//�����l
				strdata[2] = StrList[5][i];
				StrList[14] = StrList[10];
			}
			break;
		case 1:	//��������
			if((stat & 0x0000400) != 0){	//�ݒ肵����
				data[98] = data[28];
				stat ^= 0x0000400;
				stat2 ^= 0x0000200;	//�P����
				SetFont();
				if((stat & 0x40000) != 0){	//���X�\����
					makeRes(/*0*/);
				}
			} else {
				stat |= 0x0000400;
				stat2 |= 0x0000200;	//�P����
				data[26] = 2;	//��
				data[27] = 0;	//��
				data[28] = data[98];	//�����l
				strdata[2] = StrList[5][i];
				StrList[14] = StrList[35];
			}
			//System.out.println(data[98]);
			break;
		case 2:	//����̪��
			if((stat & 0x0000400) != 0){	//�ݒ肵����
				stat ^= 0x0000400;
				stat2 ^= 0x0000100;	//����
				data[99] = data[28];
				stat2 ^= 0x1000000;
			} else{
				NumSet();
				stat |= 0x0000400;
				stat2 |= 0x0000100;	//����
				data[26] = 2;	//���
				data[27] = 0;	//��
				data[28] = data[99];	//�����l
				StrList[15] = StrList[36];
				stat2 ^= 0x0002000;
				strdata[2] = StrList[5][i];

				stat2 |= 0x1000000;
			}
			break;
		case 3:	//�s��
			if((stat & 0x0000400) != 0){	//�ݒ肵����
				stat ^= 0x0000400;
				stat2 ^= 0x0000100;	//����
				data[34] = data[28];
			} else {
				stat |= 0x0000400;
				stat2 |= 0x0000100;	//����
				data[26] = 99;	//���
				data[27] = 0;	//��
				data[28] = data[34];	//�����l
				data[29] = 1;	//�����|�P
				strdata[2] = StrList[5][i];
			}
			break;
//			case 4:	//�ڈꗗ���ƭ���
		case 7:	//�w�ʉt��
			if((stat & 0x0000400) != 0){	//�ݒ肵����
				stat ^= 0x0000400;
				stat2 ^= 0x0000100;	//����
//				if(i == 4){
//				data[56] = data[28];
//				} else {
				if(data[28] == 0){
					data[57] &= ~0x00000070;
				} else {
					data[57] |= 0x00000010;

					if(data[28] >= 3){
						data[57] |= 0x00000040;
						data[28] -= 2;
					} else {
						data[57] &= ~0x00000040;
					}
					if(data[28] == 1){
						data[57] &= ~0x00000020;
					} else {
						data[57] |= 0x00000020;
					}
				}
//				data[51] = height / 2;
//				data[53] = height / 2;
				stat2 ^= 0x1000000;
//				}
				stat2 &= ~0x0002000;
			} else {
				NumSet();
//				if(i == 4){
//				data[28] = data[56];	//�����l
//
//				data[26] = 9;	//���
//				} else {	//1:mona 2:nomal 3:mona+star 4:nomal+star
				stat2 ^= 0x0002000;
				if((data[57] & 0x00000010) == 0){
					data[28] = 0;
				} else {
					if((data[57] & 0x00000020) != 0){data[28] = 2;
					} else {data[28] = 1;}
					if((data[57] & 0x00000040) != 0){data[28] += 2;}
				}
				data[26] = 4;	//���
				//strdata[7] = null;
				/*
				 StrList[15] = new String[3];
				 StrList[15][0] = "0:����";
				 StrList[15][1] = "1:Mona 2:Nomal";
				 StrList[15][2] = "3:1+�� 4:2+��";
				 */
				StrList[15] = StrList[30];
//				data[51] -= data[30] * 2;
//				data[53] += data[30] + 5;
				stat2 |= 0x1000000;
//				}
				data[29] = 0;	//�����|�P
				strdata[2] = StrList[5][i];
			}
			break;
		case 4: //�����\��
		case 5:	//�����\��
		case 6:	//�b�\��
		case 8:	//ʲ���׽�
		case 9:	//�ǎ���\��
		case 10:	//��������̕\��
		case 11:	//�ڏ��̕\��
			if((stat & 0x0000400) != 0){	//�ݒ肵����
				stat ^= 0x0000400;
				stat2 ^= 0x0000200;	//�P����
				if(data[28] == 1){
					if(i == 4){
						data[57] |= 0x00040000;
					}else if(i == 5){	//�����\��
						data[57] |= 0x00000008;
					} else if(i == 6) {	//�b�\��
						data[49] |= 0x02;
					} else if(i == 8){
						//data[57] |= 0x00000100;
						data[58] = 1;
						ColScm = ColPreset[1];
					} else if(i == 9) {
						data[57] |= 0x00000400;
					} else if(i == 10){
						data[57] |= 0x00004000;
						chkcache();
					} else if(i == 11){
						data[56] = 2;
					}
				} else {
					if(i == 4){
						data[57] &= ~0x00040000;
					}else if(i == 5){	//�����\��
						data[57] &= ~0x00000008;
					} else if(i == 6) {	//�b�\��
						data[49] &= ~0x02;
					} else if(i == 8){
						//data[57] &= ~0x00000100;
						if(data[58] == 1){
							data[58] = 0;
							ColScm = ColPreset[0];
						}
					} else if(i == 9){
						data[57] &= ~0x00000400;
					} else if(i == 10){
						data[57] &= ~0x00004000;
					} else if(i == 11){
						data[56] = 1;
					}
				}
			} else {
				stat |= 0x0000400;
				stat2 |= 0x0000200;	//�P����
				data[26] = 1;	//���
				data[27] = 0;	//��
				data[28] = 0;	//�����l

				if(i == 4){
					if((data[57] & 0x00040000) != 0)
						data[28] = 1;	//�����l
				}else if(i == 5){	//�����\��
					if((data[57] & 0x00000008) != 0)
						data[28] = 1;	//�����l
				} else if(i == 6){	//�b�\��
					if((data[49] & 0x02) != 0)
						data[28] = 1;	//�����l
				} else if(i == 8){
					if(data[58] == 1){
						data[28] = 1;	//�����l
					}
					//if((data[57] & 0x00000100) != 0)
					//	data[28] = 1;	//�����l
				} else if(i == 9){
					if((data[57] & 0x00000400) != 0){data[28] = 1;}	//�����l
				} else if(i == 10){
					if((data[57] & 0x00004000) != 0){data[28] = 1;}
					/*
					 StrList[15] = new String[3];
					 StrList[15][0] = "������̑��݂�";
					 StrList[15][1] = "���������ݸ��";
					 StrList[15][2] = "�F��ς��܂��";
					 data[51] -= data[30] * 2;
					 data[53] += data[30] + 5;
					 stat2 |= 0x1000000;
					 */
				} else if(i == 11){
					if(data[56] > 1){data[28] = 1;}
				}
				data[29] = 1;	//�����|�P
				strdata[2] = StrList[5][i];
				StrList[14] = StrList[11];
			}
			break;
		case 12:	//�d�r�Ɠd�g��ϰ�
			if((stat & 0x0000400) != 0){	//�ݒ肵����
				stat ^= 0x0000400;
				stat2 ^= 0x0000100;	//����
				data[97] = data[28];
				stat2 ^= 0x1000000;
				stat2 &= ~0x0002000;
			} else {
				NumSet();

				stat2 ^= 0x0002000;
				data[28] = data[97];
				data[26] = 99999;	//���
				data[27] = 4;	//��
				data[28] = data[97];	//�����l
				data[29] = 4;	//�����|�P
				//data[26] = 9999;	//���
				StrList[15] = StrList[34];
				stat2 |= 0x1000000;
				//data[29] = 4;	//�����|�P
				strdata[2] = StrList[5][i];
			}
			break;

		case 13:	//�߂�
			stat2 ^= 0x0000002;
			ListBox = StrList[4];
			//data[19] = ListBox.length;
			//data[10] = data[11] = 0;
			data[10] = data[20];	data[11] = data[21];
			break;
		}
	}

	/**�@
	 * ���상�j���[
	 * @param i �I�������ӏ�
	 */
	public final void contmenu(int i){
		switch(i){
			case 0:	//��۰ٗ�
			case 1:	//��۰ٕ��@(�ڗ�)
				if((stat & 0x0000400) != 0){	//�ݒ肵����
					stat ^= 0x0000400;
					stat2 ^= 0x0000100;	//����
					if(i == 0){
						data[35] = data[28];
					} else {
						data[57] &= ~0x00028000;
						if(data[28] == 1){
							data[57] |= 0x00008000;
						} else if(data[28] == 2){
							data[57] |= 0x00020000;
						}
	//					data[51] = height / 2;
	//					data[53] = height / 2;
						stat2 ^= 0x1000000;
					}
				} else {
					stat |= 0x0000400;
					stat2 |= 0x0000100;	//����
					if(i == 0){
						data[26] = 99;	//���
						data[27] = 0;	//��
						data[28] = data[35];	//�����l
						data[29] = 1;	//�����|�P
					} else {
						data[26] = 2;	//���
						data[27] = 0;	//��
						data[28] = 0;	//�����l
						if((data[57] & 0x00008000) != 0){
							data[28] = 1;	//�����l
						} else if((data[57] & 0x00020000) != 0){
							data[28] = 2;	//�����l
						}
						data[29] = 0;	//�����|�P

						StrList[15] = StrList[32];
	//					data[51] -= data[30] * 2;	data[53] += data[30] + 5;
						stat2 |= 0x1000000;
					}
					strdata[2] = StrList[6][i];
				}
				break;

			case 2:	//��Ľ�۰�
			case 3:	//SO�p��۰ُ���
			case 4://AA���[�h����6.4�L�[�ňړ�
				if((stat & 0x0000400) != 0){	//�ݒ肵����
					stat ^= 0x0000400;
					stat2 ^= 0x0000200;	//�P����
					if(data[28] == 1){
						if(i == 2){
							data[49] |= 0x01;
						} else if(i == 3){
							data[49] |= 0x20;
						} else if(i == 4) {
							data[49] |= 0x100;
						}
						//data[49] &= ~0x01;	//��Ľ�۰�
					} else {
						if(i == 2){
							data[49] &= ~0x01;
						} else if(i == 3){
							data[49] &= ~0x20;
						} else if(i == 4) {
							data[49] &= ~0x100;
						}
					}
					//�e�ʂ��҂����߂ɏ���
					/*
					 data[51] = height / 2;
					 data[53] = height / 2;
					 */
	//				stat2 ^= 0x1000000;
				} else {
					stat |= 0x0000400;
					stat2 |= 0x0000200;	//�P����
					data[26] = 1;	//���
					data[27] = 0;	//��
					data[28] = 0;	//�����l
					if(i == 2){
						if((/*stat & 0x0000200*/data[49] & 0x01) != 0){
							data[28] = 1;	//�����l
						}
					} else if(i == 3){
						if((data[49] & 0x20) != 0){
							data[28] = 1;	//�����l
						}
					} else if(i == 4){
						if((data[49] & 0x100) != 0){
							data[28] = 1;	//�����l
						}
					}
					data[29] = 1;	//�����|�P
					strdata[2] = StrList[6][i];
					StrList[14] = StrList[11];
					//�e�ʂ��҂����߂ɏ���
					/*
					 strdata[7] = null;
					 StrList[15] = new String[3];
					 StrList[15][0] = "SO���[�����g�p";
					 StrList[15][1] = "����ꍇ�́���";
					 StrList[15][2] = "���Ă��������B";
					 data[51] -= data[30] * 2;
					 data[53] += data[30] + 5;
					 stat2 |= 0x1000000;
					 */
					//�e�ʂ��҂����߂ɏ���
					/*
					 strdata[7] = null;
					 StrList[15] = new String[3];
					 StrList[15][0] = "N504i�ŕs�";
					 StrList[15][1] = "�����邩������";
					 StrList[15][2] = "�܂���B";
					 data[51] -= data[30] * 2;
					 data[53] += data[30] + 5;
					 stat2 |= 0x1000000;
					 */							}
				break;

				case 5:	//�߂�
					stat2 ^= 0x0000004;
					ListBox = StrList[4];
					//data[19] = ListBox.length;
					//data[10] = data[11] = 0;
					data[10] = data[20];	data[11] = data[21];
				break;
			}
	}

	/**
	 * ���̑��̐ݒ胁�j���[
	 * @param i �I�������ӏ�
	 */
	public final void othermenu(int i){
		switch(i){
			case 0:	//�߹�đ�̗݌v
				//strdata[7] = null;
				//StrList[15] = new String[2];
				//StrList[15][0] = "�߹�đ�̗݌v";
				//StrList[15][1] = "��" + data[47] / 100 + "." + data[47] % 100 + "�~";
				//stat2 |= 0x0001000;
				viewcost();
			break;
			case 1:	//�݌v��ؾ��
				data[47] = 0;
				//SaveSetting();

				strdata[7] = "ؾ�Ċ���";
				stat2 |= 0x0001000;
			break;
			case 2:	//�߹�đ�̒P��
			case 3:	//�߹�đ�x��
			case 4:	//��ǂ݋@�\
				if((stat & 0x0000400) != 0){	//�ݒ肵����
					stat ^= 0x0000400;
					stat2 ^= 0x0000100;	//����
					if(i == 2){
						data[69] = data[28];
					} else if(i == 3){
						data[91] = data[28];
					} else{
						data[84] = data[28];
					}
	//				data[51] = height / 2;
	//				data[53] = height / 2;
					stat2 ^= 0x1000000;
				} else {
					NumSet();
					if(i == 2){
						data[28] = data[69];	//�����l
						//strdata[7] = null;
						/*StrList[15] = new String[4];
						StrList[15][0] = "�P�ʂ�";
						StrList[15][1] = "\\/1000pckt�ł�";
						StrList[15][2] = "ex)\\0.3/p->300";
						StrList[15][3] = "0:�����ݒ�";*/
						StrList[15] = StrList[19];
					} else if(i == 3){
						data[26] = 999999;	//���
						data[29] = 5;	//�I���o���鐔���̏���̌���-1

						data[28] = data[91];	//�����l
						//strdata[7] = null;
						/*StrList[15] = new String[3];
						StrList[15][0] = "�ݒ���z�𒴂�";
						StrList[15][1] = "��Ƥ�x�����\��";
						StrList[15][2] = "����܂��";*/
						StrList[15] = StrList[20];
					} else {
						data[28] = data[84];	//�����l
						/*StrList[15] = new String[4];
						StrList[15][0] = "�����Ŏw�肵��";
						StrList[15][1] = "ڽ���O�ɂȂ��";
						StrList[15][2] = "��ǂ݂����܂�";
						StrList[15][3] = "0:��ǂ݂��Ȃ�";*/
						StrList[15] = StrList[21];
					}
	//				data[51] -= data[30] * 2;
	//				data[53] += data[30] + 5;
					stat2 ^= 0x0002000;
					strdata[2] = StrList[8][i];

					stat2 |= 0x1000000;
				}
			break;
			case 5:	//����������@�\
			case 6:	//Ұْ��M�ʒm�@�\
			case 7:	//�����폜�΍�
			case 8:	//�ŐVڽ�\��(�ڗ�)
			case 16://�������݉�ʂ�URL
			case 17://��ɃA���J�[���X
				if((stat & 0x0000400) != 0){	//�ݒ肵����
					stat ^= 0x0000400;
					stat2 ^= 0x0000200;	//�P����
					if(i == 5){
						if(data[28] == 1){
							data[57] |= 0x00000200;
						} else {
							data[57] &= ~0x00000200;
						}
					} else if(i == 6){
						if(data[28] == 1){
							data[57] |= 0x00000800;
						} else {
							data[57] &= ~0x00000800;
						}
					} else if(i == 7){
						if(data[28] == 1){
							data[57] |= 0x00002000;
						} else {
							data[57] &= ~0x00002000;
						}
					} else if(i == 8) {
						if(data[28] == 1){
							data[57] |= 0x00010000;
						} else {
							data[57] &= ~0x00010000;
						}
					} else if(i == 17){
						if(data[28] == 1){
							data[57] |= 0x00080000;
						} else {
							data[57] &= ~0x00080000;
						}
					}else {
						data[96] = data[28];
					}
	//				data[51] = height / 2;	data[53] = height / 2;
					stat2 ^= 0x1000000;
				} else {
					//strdata[7] = null;
					//StrList[15] = new String[3];
					stat |= 0x0000400;
					stat2 |= 0x0000200;	//�P����
					data[26] = 1;	//���
					data[27] = 0;	//��
					data[28] = 0;	//�����l
					if(i == 5){
						if((data[57] & 0x00000200) != 0){
							data[28] = 1;	//�����l
						}
						/*StrList[15][0] = "ڽ��ǂݍ��ނ�";
						StrList[15][1] = "�ޯ�ϰ�������";
						StrList[15][2] = "�ōX�V���܂��B";*/
						StrList[15] = StrList[22];
					} else if(i == 6){
						if((data[57] & 0x00000800) != 0){
							data[28] = 1;	//�����l
						}
						/*StrList[15][0] = "Ұْ��M->���f";
						StrList[15][1] = "iappli x504,5i";
						StrList[15][2] = "ez+ phase2.5�p";*/
						StrList[15] = StrList[23];
					} else if(i == 7){
						if((data[57] & 0x00002000) != 0){
							data[28] = 1;	//�����l
						}
						/*StrList[15][0] = "�����폜����";
						StrList[15][1] = "�ŐVڽ��ǂ�";
						StrList[15][2] = "���݂܂��B";*/
						StrList[15] = StrList[24];
					} else if(i == 8) {
						if((data[57] & 0x00010000) != 0){
							data[28] = 1;	//�����l
						}
						StrList[15] = StrList[31];
					} else if(i == 17){
						if((data[57] & 0x00080000) != 0){
							data[28] = 1;	//�����l
						}
						StrList[15] = StrList[38];
					}else {
						data[28] = data[96];	//�����l
						StrList[15] = StrList[33];
					}
					data[29] = 1;	//�����|�P
					strdata[2] = StrList[8][i];
					StrList[14] = StrList[11];
	//				data[51] -= data[30] * 2;	data[53] += data[30] + 5;
					stat2 |= 0x1000000;
				}
			break;

			case 9:	//7���̋@�\(�ڗ�)
				stat4 |= 0x0000100;
				ListBox = StrList[2];
				data[19] = data[10];
				data[10] = data[94];
				data[11] = 0;
	//			strdata[7] = "�@�\��I�����Ă�������";
	//			stat2 |= 0x0001000;
			break;

			case 10:	//7���̋@�\(�޸ϗ�)
				stat4 |= 0x0000400;
				ListBox = StrList[17];
				data[19] = data[10];
				data[10] = data[71];
				data[11] = 0;
	//			strdata[7] = "�@�\��I�����Ă�������";
	//			stat2 |= 0x0001000;
			break;

			case 11:	//7���̋@�\(ڽ��)
				stat4 |= 0x0001000;
				ListBox = StrList[3];
				data[19] = data[10];
				data[10] = data[73];
				data[11] = 0;
	//			strdata[7] = "�@�\��I�����Ă�������";
	//			stat2 |= 0x0001000;
			break;

			case 12:	//0���̋@�\
				stat4 |= 0x0008000;
				ListBox = StrList[37];
				data[19] = data[10];
				data[10] = data[74];
				data[11] = 0;
	//			strdata[7] = "�@�\��I�����Ă�������";
	//			stat2 |= 0x0001000;
			break;

			case 13:    //���ް�ݒ�
			case 14:    //�g����߼��
			case 15:    //������ݸ
			case 19:    //NGܰ��
			//case 17:   //�޸ޕ�
				inputForm = new Form(StrList[8][i]);
				inputForm.addCommand(command[4]);
				inputForm.addCommand(command[2]);
				if(i == 13){
					inputForm.append(new StringItem(">���݂̐ڑ���",""));
					//inputForm.append(new StringItem(server,""));
					choice = new ChoiceGroup(server+"\n>�ڑ���̕ύX", Choice.EXCLUSIVE);
					choice.append("zuzu�I��Ҕ�\n("+server_url[0]+")", null);
					choice.append("��ҎI\n("+server_url[1]+")", null);
					choice.append("zuzu�I�Ǝ���\n("+server_url[2]+")", null);
					choice.append("�蓮�ݒ�", null);
					inputForm.append(choice);
					tfield = new LocalizedTextField(">�蓮�ݒ�̱��ڽ", server, 300, LocalizedTextField.URL);
					stat2 |= 0x0080000;
					inputForm.append(tfield);
				} else if(i == 14) {
					tfield = new LocalizedTextField("�ݒ�", extendedoption, 200, LocalizedTextField.URL);
					stat2 |= 0x10000000;
					inputForm.append(tfield);
				} else if(i == 15) {
					tfield = new LocalizedTextField("����݂�URL", cushionlink, 300, LocalizedTextField.URL);
					tfield2 = new LocalizedTextField("������ݸ�̈ꗗ", cushionlinklist, 30000, LocalizedTextField.ANY);
					stat4 |= 0x00004000;
					inputForm.append(tfield);
					inputForm.append(tfield2);
				}else if(i == 18){
					tfield = new LocalizedTextField("NGܰ��", "������", 4096, LocalizedTextField.ANY);
					stat4 |= 0x00020000;
					inputForm.append(tfield);
				}/* else {
					tfield = new LocalizedTextField("�޸ޕ�", bagdata, 30000, LocalizedTextField.ANY);
					inputForm.append(tfield);
				}*/
				inputForm.setCommandListener(this);
				disp.setCurrent(inputForm);
			break;

			case 18:	//�گ�ޑ��x
				if((stat & 0x0000400) != 0){	//�ݒ肵����
					stat ^= 0x0000400;
					stat2 ^= 0x0000100;	//����
					data[82] = data[28];
					stat2 ^= 0x0002000;
				} else {
					NumSet();
					data[28] = data[82];	//�����l
					strdata[2] = StrList[8][i];

				}
			break;

			case 20:
				stat2 &= ~0x0004000;	//function����

				try{
					/*String buf = "";
					for(i = 0; i < data[85]; i++){
						buf = buf + CacheResData[nCacheIndex][i][0]  + "\n";
					}
					tbox = new LocalizedTextBox("ڽ", buf, buf.length(), LocalizedTextField.ANY);*/
					//System.out.println("\r\n\r\n-------" + resstr + "------\r\n\r\n");
					tbox = new LocalizedTextBox("�޸ޕ�", bagdata.toString(), bagdata.length() + 1, LocalizedTextField.ANY);
					//inputForm = new Form(StrList[3][i]);
					//inputForm.addCommand(command[4]);
					tbox.addCommand(command[2]);
					stat3 |= 0x0002000;
					//inputForm.append(tfield);
					//inputForm.append(new TextBox("ڽ", buf, buf.length(), TextField.ANY));
					tbox.setCommandListener(this);
					disp.setCurrent(tbox);
				} catch(Exception e){
					stat3 &= ~0x0002000;
					System.gc();
					disp.setCurrent(this);
				}
				stat |= 0x1000000;	//��ʍX�V
			break;

			/*case 18:	//�ޯ�ϰ�������
				try {
					RecordStore.deleteRecordStore("Bookmark");
				} catch(Exception e){}
				Load();
				strdata[7] = "�޸ς�����������";//StrList[10][11] + StrList[10][19];
				stat2 |= 0x0001000;
			break;
			*/
			case 21:	//������
				try {
					RecordStore.deleteRecordStore("Setting");
					RecordStore.deleteRecordStore("Bookmark");
				} catch(Exception e){}
				Load();
				strdata[7] = "����������";//StrList[10][11] + StrList[10][19];
				stat2 |= 0x0001000;

			break;
			case 22:	//���������	//���ϯ��
			/*
				strdata[7] = null;
				StrList[15] = new String[5];
				StrList[15][0] = "Map:sboooooooo";
				StrList[15][1] = "s:system";
				StrList[15][2] = "b:�ޯ�ϰ�";
				StrList[15][3] = "x:�g�p��";
				StrList[15][4] = "o:��";
				stat2 |= 0x0001000;
			*/
	//			strdata[7] = "�H����m(_ _)m";
	//			stat2 |= 0x0001000;
				System.gc();
				Runtime runtime = Runtime.getRuntime();
				//strdata[7] = null;
				StrList[15] = new String[3];
				StrList[15][0] = "��؏��";
				StrList[15][1] = "free:" + runtime.freeMemory();
				StrList[15][2] = "total:" + runtime.totalMemory();
				stat2 |= 0x0001000;
				//stat |= 0x1000000;	//��ʍX�V
			break;

			case 23:	//�߂�

				stat2 ^= 0x0000020;
				ListBox = StrList[4];
				//data[19] = ListBox.length;
				//data[10] = data[11] = 0;
				data[10] = data[20];	data[11] = data[21];
			break;
		}
		stat |= 0x1000000;	//��ʍX�V
	}

	/**
	 * �ʐM�ݒ胁�j���[
	 * @param i �I�������ӏ�
	 */
	private final void networksetting(int i) {
		switch(i){
		//���k���̒l��ς���Ƃ��͒��ӁI
		case 0:	//1��ɓǂ޽ڐ�
		case 1:	//1��ɓǂ�ڽ��
			if((stat & 0x0000400) != 0){	//�ݒ肵����
				stat ^= 0x0000400;
				stat2 ^= 0x0000100;	//����
				data[i] = data[28];
				stat2 ^= 0x0002000;
			} else {
				NumSet();
				data[28] = data[i];	//�����l
				if(i == 2){
					data[26] = 3;	//���
					data[29] = 0;	//�����|�P
				}
				strdata[2] = StrList[7][i];
			}
		break;

		case 2:		//�ŐVڽ�œǂސ�
		case 3:		//gzip���k
		case 4:		//iMona���k
		case 9:		//AA�̕\��
		case 10:	//URL�̕\��
			if((stat & 0x0000400) != 0){	//�ݒ肵����
				stat ^= 0x0000400;
				stat2 &= ~0x0000F00;
				if(i == 2){
					data[55] = data[28];
				} else if(i == 3){
					data[80] = data[28];
				} else if(i == 4){
					data[76] = data[28];
				} else if(i == 9){
					data[87] &= ~0xFF;
					data[87] |= data[28];
				} else {	//i == 10
					data[87] &= ~0xFF00;
					data[87] |= data[28] << 8;
				}
				stat2 &= ~0x0002000;
				data[51] = height / 2;
//				data[53] = height / 2;
				stat2 &= ~0x1000000;
			} else {
				NumSet();
				//stat |= 0x0000400;
				//stat2 |= 0x0002100;	//���������
				//stat2 |= 0x0000100;	//�����ݒ�
				//stat2 |= 0x0002000;	//�����ݒ�ł̍ł��Ⴂ������1�ɂ���t���O
				data[29] = 0;	//�I���o���鐔���̏���̌���-1
				//strdata[7] = null;
				//strdata[7] = null;
				if(i == 2){
					stat2 ^= 0x0002000;		//�����ݒ�ł̍ł��Ⴂ������1�ɂ���t���O
					data[29] = 3;	//�I���o���鐔���̏���̌���-1

					data[28] = data[55];	//�����l
					/*StrList[15] = new String[4];
					StrList[15][0] = "0���w�肷���";
					StrList[15][1] = "1��ɓǂ�ڽ��";
					StrList[15][2] = "�Ɠ����l���g�p";
					StrList[15][3] = "���܂��B";*/
					StrList[15] = StrList[25];
				} else if(i == 3){
					stat2 ^= 0x0002000;		//�����ݒ�ł̍ł��Ⴂ������1�ɂ���t���O
					data[28] = data[80];	//�����l
					data[26] = 9;	//���
					/*StrList[15] = new String[4];
					StrList[15][0] = "x505i,japp�p";
					StrList[15][1] = "0:���k�Ȃ�";
					StrList[15][2] = "�e��:1 > 9";
					StrList[15][3] = "�����l:6";*/
					StrList[15] = StrList[26];
				} else if(i == 4){
					data[28] = data[76];	//�����l
					data[26] = 3;	//���
/*									StrList[15] = new String[3];
					StrList[15][0] = "1:��t�̂�";
					StrList[15][1] = "2:����߰�";
					StrList[15][2] = "3:���k�Ȃ�";
*/
					StrList[15] = StrList[27];
				} else if(i == 9){
					//if((data[57] & 0x00000002) != 0){data[28] = 1;}
					data[28] = data[87] & 0xFF;
					data[26] = 5;	//���

					/*StrList[15] = new String[5];
					StrList[15][0] = "1:�S���\������";
					StrList[15][1] = "2:AA�̂ݏ���";
					StrList[15][2] = "3:AA�̂ݏ���2";
					StrList[15][3] = "4:2+�S����";
					StrList[15][4] = "5:3+�V";*/
					StrList[15] = StrList[28];
				} else {	//i == 10
					data[28] = (data[87] & 0xFF00) >> 8;
					//if((data[57] & 0x00004000) != 0){
					//	data[28] = 2;
					//} else if((data[57] & 0x00000004) != 0){
					//	data[28] = 1;
					//}

					data[26] = 4;	//���

					/*StrList[15] = new String[4];
					StrList[15][0] = "1:�S���\������";
					StrList[15][1] = "2:�ȗ�����";
					StrList[15][2] = "3:2ch���̂ݕ\��";
					StrList[15][3] = "4:���S����";*/
					StrList[15] = StrList[29];
				}
				//data[51] -= data[30] * 2;
				data[51] -= /*data[30] * 2 + */data[33];
//				data[53] += data[30] + 5;
				stat2 |= 0x1000000;
				strdata[2] = StrList[7][i];
			}
		break;
		//����4�̒l��ς���Ƃ��͒��ӁI
		case 5:	//ID�̕\��
		case 6:	//�����̕\��
		case 7:	//ұ�ނ̕\��
		case 8:	//���O�̕\��
			//i = data[10] + data[11];

			if((stat & 0x0000400) != 0){	//�ݒ肵����
				stat ^= 0x0000400;
				stat2 &= ~0x0000F00;
				//if(i == 9){
				//	data[28] = data[87];
				//} else
				if(data[28] == 1){
					if(i == 5){
						data[49] |= 0x04;
					} else if(i == 6){
						data[49] |= 0x08;
					} else if(i == 7){
						data[49] |= 0x10;
					} else if(i == 8){
						data[49] &= ~0x40;
					//} else if(i == 9){
					//	data[57] |= 0x00000002;
					//} else if(i == 10){
					//	data[57] &= ~0x00004000;
					//	data[57] |= 0x00000004;
					}
				//} else if(data[28] == 2) {
				//	data[57] |= 0x00004000;
				} else {	//data[28] == 0
					if(i == 5){
						data[49] &= ~0x04;
					} else if(i == 6){
						data[49] &= ~0x08;
					} else if(i == 7){
						data[49] &= ~0x10;
					} else if(i == 8){
						data[49] |= 0x40;
					//} else if(i == 9){
					//	data[57] &= ~0x00000002;
					//} else if(i == 10){
					//	data[57] &= ~0x00004004;
					}
				}

//				data[51] = height / 2;
//				data[53] = height / 2;
				stat2 &= ~0x1000000;
			} else {
				stat |= 0x0000400;
				stat2 |= 0x0000200;	//�P����
				data[26] = 1;	//���
				data[27] = 0;	//��

				data[28] = 0;	//�����l
				data[29] = 1;	//�����|�P
				if(i == 5){
					if((data[49] & 0x04) != 0){data[28] = 1;}
				} else if(i == 6){
					if((data[49] & 0x08) != 0){data[28] = 1;}
				} else if(i == 7){
					if((data[49] & 0x10) != 0){data[28] = 1;}
				} else if(i == 8){
					if((data[49] & 0x40) == 0){data[28] = 1;}

				}
				strdata[2] = StrList[7][i];
				StrList[14] = StrList[11];
			}
		break;

		case 11:	//�߂�
			stat2 ^= 0x0000008;
			ListBox = StrList[4];
			//data[19] = ListBox.length;
			//data[10] = data[11] = 0;
			data[10] = data[20];	data[11] = data[21];
		break;
		}
	}

	/**
	 * 0�L�[�V���[�g�J�b�g�p�̉B�����j���[
	 * @param i �I�������ӏ�
	 */
	private final void orezimenu(int i) {
		stat4 |= 0x0010000;
		switch(i){
			case 0:	//�����T�C�Y�ύX(��)
				if(data[36] == 0){
					data[36] = 1;
				}else if(data[36] == 1){
					data[36] = 2;
				}else if(data[36] == 2){
					data[36] = 0;
				}
				SetFont();
				if((stat & 0x40000) != 0){	//���X�\����
					makeRes(/*0*/);
				}
				//System.out.println("��������");
			break;
			case 1: //������ݸ��g�p�ެ���(ڽ)
				if( (stat & 0x40000) != 0 ){	//���X�̕\��
					Link(null, 0);
				}
				//System.out.println("�񸯼���ݸ�ެ���");
			break;
			case 2://����������ONtoOFF
				if((data[57] & 0x00000200) != 0){
					data[57] ^= 0x00000200;
					strdata[7] = "����������OFF";
				}else{
					data[57] |= 0x00000200;
					strdata[7] = "����������ON";
				}
				stat2 |= 0x0001000;
			break;
			case 3:
				if( (stat & 0x40000) != 0 ){	//ڽ�\����
					if(-1 == EditBookMark( 0, CacheTitle[nCacheIndex]/*strdata[9]*//*ThreadName[data[4]]*/, nCacheBrd[nCacheIndex]/*data[3]*/, nCacheTh[nCacheIndex][0]/*data[2]*//*nThread[data[4]]*/, data[6]+nCacheSt[nCacheIndex]/*data[7]*/)){
						//strdata[7] = null;
						//StrList[15] = new String[3];
						//StrList[15][0] = "= �o�^���s =";
						//StrList[15][1] = "�ޯ�ϰ�������";
						//StrList[15][2] = "�ς��ł��B";
						strdata[7] = "�o�^���s";
						stat2 |= 0x0001000;
					}
				}else if((stat & 0x0010000) != 0){//�ڕ\����
					//if(-1 == EditBookMark( 0, CacheBrdData[nCacheIndex]/*ThreadName*/[data[4]], nCacheBrd[nCacheIndex]/*data[3]*/, nCacheTh[nCacheIndex]/*nThread*//*nThread*/[data[4]], 0)){
					if(-1 == EditBookMark( 0, CacheBrdData[nCacheIndex]/*ThreadName*/[data[60]], nCacheBrd[nCacheIndex]/*data[3]*/, nCacheTh[nCacheIndex]/*nThread*//*nThread*/[data[60]], 0)){
						//strdata[7] = null;
						//StrList[15] = new String[3];
						//StrList[15][0] = "= �o�^���s =";
						//StrList[15][1] = "�ޯ�ϰ�������";
						//StrList[15][2] = "�ς��ł��B";
						strdata[7] = "�o�^���s";
						stat2 |= 0x0001000;
					}
				}else if( (stat & 0x0004000) != 0 ){	//���X�g�ɂ���Ƃ�
					EditBookMark( 0,"[��]" + ListBox[data[10]+data[11]], ((data[22] + data[23]) * 100 + data[10] + data[11]), 0, 0);
				}
			break;
			case 4:
				httpinit(0,0,0,0,0);//dlbrdlist();
			break;
		}
		stat |= 0x1000000;	//��ʍX�V
		stat4 ^= 0x0010000;
	}

	/**
	 * zuzu���ǉ��������O�p�����B
	 * @param text �\���������e�L�X�g�B
	 */
	private final void Bugln(String text) {
		if(bagdata.length() >= 4096 || bagdata.length() <= 0){
			bagdata = null;
			bagdata = new StringBuffer("");
			bagdata.append("Connect:" + server + "\nVersion:" + version + "\n");
			System.out.print("Connect:" + server + "\nVersion:" + version + "\n");
		}
		bagdata.append(text);
		System.out.print(text);
	}


} //class MainCanvas�̏I���