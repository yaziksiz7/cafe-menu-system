package KafeMenu;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.text.ParseException;
import java.util.StringJoiner;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import KafeMenu.Bilesenler.Alan;
import KafeMenu.Bilesenler.Kutu;

/** Yeni malzeme ekleme ve mevcut malzemeyi düzenleme penceresi. */
final class MalzemeDialog extends Diyaloglar.FormDialog {

	private final AnaPencere ana;
	private final Malzeme mevcut;

	private final Alan adAlani = new Alan("ör. Badem sütü", null);
	private final JComboBox<String> birimKutusu = Bilesenler.secimKutusu(new JComboBox<>(Malzeme.BIRIMLER));
	private final JSpinner stokAlani = Bilesenler.spinner(
			new JSpinner(new SpinnerNumberModel(0d, 0d, 10_000_000d, 1d)), "#,##0.##");
	private final JSpinner minStokAlani = Bilesenler.spinner(
			new JSpinner(new SpinnerNumberModel(0d, 0d, 10_000_000d, 1d)), "#,##0.##");
	private final JCheckBox[] alerjenKutulari = new JCheckBox[Malzeme.ALERJENLER.length];
	private final JCheckBox veganKutusu = kutu("Vegan (hayvansal ürün içermez)");
	private final JCheckBox vejetaryenKutusu = kutu("Vejetaryen (et ve balık içermez)");

	private int kaydedilenId;
	private String kaydedilenAd;

	MalzemeDialog(AnaPencere ana, Malzeme mevcut) {
		super(ana, mevcut == null ? "Yeni Malzeme" : "Malzemeyi Düzenle",
				mevcut == null ? "Mutfak stoğuna yeni bir malzeme ekleyin" : mevcut.kod() + " · " + mevcut.ad(),
				mevcut == null ? Simge.Tur.ARTI : Simge.Tur.KALEM,
				mevcut == null ? "Malzemeyi Ekle" : "Değişiklikleri Kaydet", Tema.KARAMEL);
		this.ana = ana;
		this.mevcut = mevcut;
		birimKutusu.setEditable(true);

		JPanel satir1 = new JPanel(new BorderLayout(12, 0));
		satir1.setOpaque(false);
		satir1.add(Bilesenler.alanBlok("Malzeme adı", adAlani), BorderLayout.CENTER);
		JPanel birimBlok = Bilesenler.alanBlok("Birim", birimKutusu);
		birimBlok.setPreferredSize(new Dimension(150, birimBlok.getPreferredSize().height));
		satir1.add(birimBlok, BorderLayout.EAST);
		satir1.setAlignmentX(Component.LEFT_ALIGNMENT);
		govde.add(satir1);
		govde.add(Box.createVerticalStrut(14));

		JPanel satir2 = new JPanel(new GridLayout(1, 2, 12, 0));
		satir2.setOpaque(false);
		satir2.add(Bilesenler.alanBlok("Stok miktarı", stokAlani));
		satir2.add(Bilesenler.alanBlok("Kritik stok seviyesi", minStokAlani));
		satir2.setAlignmentX(Component.LEFT_ALIGNMENT);
		govde.add(satir2);
		govde.add(Box.createVerticalStrut(20));

		govde.add(Bilesenler.bolumBasligi("Alerjenler"));
		govde.add(Box.createVerticalStrut(8));
		JPanel alerjenler = new JPanel(new GridLayout(0, 3, 10, 4));
		alerjenler.setOpaque(false);
		for (int i = 0; i < Malzeme.ALERJENLER.length; i++) {
			alerjenKutulari[i] = kutu(Malzeme.ALERJENLER[i]);
			alerjenler.add(alerjenKutulari[i]);
		}
		alerjenler.setAlignmentX(Component.LEFT_ALIGNMENT);
		govde.add(alerjenler);
		govde.add(Box.createVerticalStrut(18));

		govde.add(Bilesenler.bolumBasligi("Beslenme"));
		govde.add(Box.createVerticalStrut(8));
		JPanel beslenme = Bilesenler.yatay(20, veganKutusu, vejetaryenKutusu);
		beslenme.setAlignmentX(Component.LEFT_ALIGNMENT);
		govde.add(beslenme);
		veganKutusu.addActionListener(e -> veganDegisti());

		if (mevcut != null) {
			adAlani.setText(mevcut.ad());
			birimKutusu.setSelectedItem(mevcut.birim());
			stokAlani.setValue(mevcut.stok());
			minStokAlani.setValue(mevcut.minStok());
			if (mevcut.alerjen() != null) {
				for (String a : mevcut.alerjen().split(",")) {
					for (int i = 0; i < Malzeme.ALERJENLER.length; i++) {
						if (Malzeme.ALERJENLER[i].equalsIgnoreCase(a.trim())) alerjenKutulari[i].setSelected(true);
					}
				}
			}
			veganKutusu.setSelected(mevcut.vegan());
			vejetaryenKutusu.setSelected(mevcut.vejetaryen());
			if (mevcut.kullanimSayisi() > 0) {
				govde.add(Box.createVerticalStrut(16));
				Kutu bilgi = new Kutu(new BorderLayout(), Tema.MAVI_ACIK);
				bilgi.add(Bilesenler.paragraf("Bu malzeme <b>" + mevcut.kullanimSayisi() + " üründe</b> kullanılıyor. "
						+ "Alerjen ve vegan/vejetaryen değişiklikleri bu ürünlere otomatik yansır.", 470, 12.5f,
						Tema.METIN));
				bilgi.setAlignmentX(Component.LEFT_ALIGNMENT);
				govde.add(bilgi);
			}
		} else {
			veganKutusu.setSelected(true);
			vejetaryenKutusu.setSelected(true);
		}
		veganDegisti();
		SwingUtilities.invokeLater(adAlani::requestFocusInWindow);
	}

	private static JCheckBox kutu(String metin) {
		JCheckBox k = new JCheckBox(metin);
		k.setOpaque(false);
		k.setFont(Tema.font(Font.PLAIN, 13f));
		k.setForeground(Tema.METIN);
		k.setFocusPainted(false);
		return k;
	}

	/** Vegan olan bir malzeme otomatik olarak vejetaryendir. */
	private void veganDegisti() {
		if (veganKutusu.isSelected()) vejetaryenKutusu.setSelected(true);
		vejetaryenKutusu.setEnabled(!veganKutusu.isSelected());
	}

	int kaydedilenId() {
		return kaydedilenId;
	}

	String kaydedilenAd() {
		return kaydedilenAd;
	}

	@Override
	protected String kaydet() throws Exception {
		String ad = adAlani.getText().trim();
		if (ad.isEmpty()) return "Malzeme adı boş olamaz.";
		if (ad.length() > 100) return "Malzeme adı en fazla 100 karakter olabilir.";
		Object birimSecimi = birimKutusu.getEditor().getItem();
		String birim = birimSecimi == null ? "" : birimSecimi.toString().trim();
		if (birim.isEmpty()) return "Birim boş olamaz (ör. gr, ml, adet).";
		if (birim.length() > 20) return "Birim en fazla 20 karakter olabilir.";
		try {
			stokAlani.commitEdit();
			minStokAlani.commitEdit();
		} catch (ParseException e) {
			return "Stok değerlerini sayı olarak yazın.";
		}
		StringJoiner alerjen = new StringJoiner(", ");
		for (JCheckBox k : alerjenKutulari) {
			if (k.isSelected()) alerjen.add(k.getText());
		}
		Malzeme m = new Malzeme(mevcut == null ? 0 : mevcut.id(), ad, birim,
				((Number) stokAlani.getValue()).doubleValue(), ((Number) minStokAlani.getValue()).doubleValue(),
				alerjen.length() == 0 ? null : alerjen.toString(), veganKutusu.isSelected(),
				vejetaryenKutusu.isSelected(), 0);
		kaydedilenId = ana.db.malzemeKaydet(m);
		kaydedilenAd = ad;
		return null;
	}
}
