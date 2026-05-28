import { Plus_Jakarta_Sans, JetBrains_Mono } from "next/font/google";
import "./globals.css";
import { Providers } from '@/providers/providers.jsx';
import AuthPanel from "@/components/AuthPanel/AuthPanel";

const jakartaSans = Plus_Jakarta_Sans({
    variable: "--font-sans",
    subsets: ["latin", "latin-ext"],
});

const jetbrainsMono = JetBrains_Mono({
    variable: "--font-mono",
    subsets: ["latin", "latin-ext"],
});

export const metadata = {
    title: "Tripify",
    description: "Travels made simple",
};

export default function RootLayout({ children }) {
    return (
        <html
            lang="en"
            suppressHydrationWarning
        >
            <body className={`min-h-screen w-full ${jakartaSans.variable} ${jetbrainsMono.variable} font-sans antialiased`}>
                <Providers>
                    <AuthPanel />
                    {children}
                </Providers>
            </body>
        </html>
    );
}
