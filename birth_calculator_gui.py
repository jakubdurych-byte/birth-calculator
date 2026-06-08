import tkinter as tk
from tkinter import messagebox
from datetime import datetime

class BirthdayCalculatorGUI:
    def __init__(self, root):
        self.root = root
        self.root.title("Kalkulátor Věku")
        self.root.geometry("500x600")
        self.root.configure(bg="#f0f0f0")
        
        # Hlavní nadpis
        title_frame = tk.Frame(root, bg="#2c3e50")
        title_frame.pack(fill=tk.X)
        
        title_label = tk.Label(
            title_frame, 
            text="KALKULÁTOR VĚKU",
            font=("Arial", 18, "bold"),
            bg="#2c3e50",
            fg="white",
            pady=15
        )
        title_label.pack()
        
        # Hlavní kontejner
        main_frame = tk.Frame(root, bg="#f0f0f0")
        main_frame.pack(fill=tk.BOTH, expand=True, padx=20, pady=20)
        
        # Instrukce
        instruction_label = tk.Label(
            main_frame,
            text="Zadejte své datum narození",
            font=("Arial", 12),
            bg="#f0f0f0"
        )
        instruction_label.pack(pady=(0, 20))
        
        # Rámec pro vstup data
        input_frame = tk.Frame(main_frame, bg="white", relief=tk.SUNKEN, bd=1)
        input_frame.pack(fill=tk.X, pady=10)
        
        # Popisek pro datum
        format_label = tk.Label(
            input_frame,
            text="Formát: dd.mm.yyyy",
            font=("Arial", 9),
            bg="white",
            fg="#7f8c8d"
        )
        format_label.pack(anchor="w", padx=10, pady=(5, 0))
        
        # Pole pro vstup
        self.date_entry = tk.Entry(
            input_frame,
            font=("Arial", 14),
            justify=tk.CENTER,
            bg="white"
        )
        self.date_entry.pack(fill=tk.X, padx=10, pady=10)
        self.date_entry.bind("<Return>", lambda e: self.calculate())
        
        # Tlačítko Vypočítaj
        self.calculate_button = tk.Button(
            main_frame,
            text="VYPOČÍTEJ",
            font=("Arial", 12, "bold"),
            bg="#27ae60",
            fg="white",
            pady=10,
            cursor="hand2",
            command=self.calculate
        )
        self.calculate_button.pack(fill=tk.X, pady=15)
        
        # Oddělující čára
        separator = tk.Frame(main_frame, bg="#bdc3c7", height=2)
        separator.pack(fill=tk.X, pady=15)
        
        # Rámec pro výsledky
        results_frame = tk.Frame(main_frame, bg="white", relief=tk.SUNKEN, bd=1)
        results_frame.pack(fill=tk.BOTH, expand=True, pady=10)
        
        # Nadpis výsledků
        results_title = tk.Label(
            results_frame,
            text="VÝSLEDKY:",
            font=("Arial", 12, "bold"),
            bg="white"
        )
        results_title.pack(anchor="w", padx=15, pady=(10, 5))
        
        # Textové pole pro výsledky
        self.results_text = tk.Text(
            results_frame,
            font=("Courier", 10),
            height=15,
            width=50,
            bg="white",
            relief=tk.FLAT,
            state=tk.DISABLED
        )
        self.results_text.pack(fill=tk.BOTH, expand=True, padx=10, pady=10)
        
        # Scrollbar pro textové pole
        scrollbar = tk.Scrollbar(
            results_frame,
            command=self.results_text.yview
        )
        scrollbar.pack(side=tk.RIGHT, fill=tk.Y)
        self.results_text.config(yscrollcommand=scrollbar.set)
        
        # Tlačítko Vymazat
        clear_button = tk.Button(
            main_frame,
            text="VYMAZAT",
            font=("Arial", 10),
            bg="#e74c3c",
            fg="white",
            pady=8,
            cursor="hand2",
            command=self.clear
        )
        clear_button.pack(fill=tk.X, pady=10)
    
    def validate_date(self, date_str):
        """
        Ověří formát data a vrací objektu datetime nebo None
        """
        if len(date_str) != 10:
            return None
        
        if date_str[2] != '.' or date_str[5] != '.':
            return None
        
        try:
            parts = date_str.split('.')
            if len(parts) != 3:
                return None
            
            day = int(parts[0])
            month = int(parts[1])
            year = int(parts[2])
            
            date_obj = datetime(year, month, day)
            
            # Kontrola, zda není v budoucnosti
            if date_obj > datetime.now():
                messagebox.showerror("Chyba", "Datum narození nemůže být v budoucnosti!")
                return None
            
            return date_obj
        
        except ValueError:
            return None
    
    def calculate(self):
        """
        Vypočítá věk a zobrazí výsledky
        """
        date_str = self.date_entry.get().strip()
        
        if not date_str:
            messagebox.showwarning("Upozornění", "Prosím, zadejte datum narození!")
            return
        
        birth_date = self.validate_date(date_str)
        
        if birth_date is None:
            messagebox.showerror(
                "Chyba",
                "Nesprávný formát data!\n\nVerezujte: dd.mm.yyyy\nPříklad: 15.03.2000"
            )
            return
        
        # Výpočty
        today = datetime.now()
        difference = today - birth_date
        
        days = difference.days
        seconds = int(difference.total_seconds())
        
        years = days // 365
        remaining_days = days % 365
        
        hours = days * 24
        minutes = days * 24 * 60
        
        # Formátování výsledků
        results = f"""
Datum narození: {birth_date.strftime('%d.%m.%Y')}
Dnešní datum:   {today.strftime('%d.%m.%Y %H:%M:%S')}

{'='*45}
VĚK: {years} let a {remaining_days} dní
{'='*45}

DETAILNĚ:
  • Dní:          {days:>15,}
  • Hodin:        {hours:>15,}
  • Minut:        {minutes:>15,}
  • Sekund:       {seconds:>15,}

CELKEM:
  • {days:,} dní
  • {hours:,} hodin
  • {minutes:,} minut
  • {seconds:,} sekund
"""
        
        # Zobrazení výsledků
        self.results_text.config(state=tk.NORMAL)
        self.results_text.delete(1.0, tk.END)
        self.results_text.insert(tk.END, results.strip())
        self.results_text.config(state=tk.DISABLED)
    
    def clear(self):
        """
        Vymaže vstup a výsledky
        """
        self.date_entry.delete(0, tk.END)
        self.results_text.config(state=tk.NORMAL)
        self.results_text.delete(1.0, tk.END)
        self.results_text.config(state=tk.DISABLED)
        self.date_entry.focus()

if __name__ == "__main__":
    root = tk.Tk()
    app = BirthdayCalculatorGUI(root)
    root.mainloop()
