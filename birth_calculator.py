from datetime import datetime

def nacti_datum():
    """
    Načte datum narození od uživatele ve formátu dd.mm.yyyy
    Opakuje vstup, dokud není datum zadáno správně
    """
    while True:
        try:
            datum_str = input("Zadejte své datum narození (formát dd.mm.yyyy): ")
            
            # Ověření formátu
            if len(datum_str) != 10 or datum_str[2] != '.' or datum_str[5] != '.':
                raise ValueError("Nesprávný formát! Použijte formát dd.mm.yyyy")
            
            # Rozparsování na jednotlivé části
            den_str, mesic_str, rok_str = datum_str.split('.')
            
            # Konverze na čísla
            den = int(den_str)
            mesic = int(mesic_str)
            rok = int(rok_str)
            
            # Vytvoření objektu datetime
            datum_narozeni = datetime(rok, mesic, den)
            
            # Ověření, že datum není v budoucnosti
            dnes = datetime.now()
            if datum_narozeni > dnes:
                raise ValueError("Datum narození nemůže být v budoucnosti!")
            
            return datum_narozeni
        
        except ValueError as e:
            print(f"Chyba: {e}")
            print("Prosím, zkuste znovu.\n")
        except Exception as e:
            print(f"Chyba při zpracování data: {e}")
            print("Prosím, zkuste znovu.\n")

def vypocitej_cas():
    """
    Hlavní funkce - načte datum a vypočítá uplynulý čas
    """
    print("=" * 50)
    print("KALKULÁTOR VĚKU - Počítač dní a sekund")
    print("=" * 50)
    print()
    
    # Načtení datumu
    datum_narozeni = nacti_datum()
    
    # Výpočet aktuálního času
    dnes = datetime.now()
    
    # Výpočet rozdílu
    rozdil = dnes - datum_narozeni
    
    # Počet dní
    pocet_dnu = rozdil.days
    
    # Počet sekund (včetně zbytku ze sekund v časovém rozdílu)
    celkem_sekund = int(rozdil.total_seconds())
    
    # Vypočítání věku v letech, měsících (přibližně)
    vek_v_letech = pocet_dnu // 365
    zbyle_dny = pocet_dnu % 365
    
    # Výstup
    print()
    print("=" * 50)
    print("VÝSLEDKY:")
    print("=" * 50)
    print(f"Datum narození: {datum_narozeni.strftime('%d.%m.%Y')}")
    print(f"Dnešní datum: {dnes.strftime('%d.%m.%Y %H:%M:%S')}")
    print()
    print(f"Váš věk: {vek_v_letech} let a {zbyle_dny} dní")
    print(f"Celkem dní: {pocet_dnu:,}")
    print(f"Celkem sekund: {celkem_sekund:,}")
    print()
    print(f"Více konkrétně:")
    print(f"  - {pocet_dnu} dní")
    print(f"  - {pocet_dnu * 24:,} hodin")
    print(f"  - {pocet_dnu * 24 * 60:,} minut")
    print(f"  - {celkem_sekund:,} sekund")
    print("=" * 50)

if __name__ == "__main__":
    vypocitej_cas()
