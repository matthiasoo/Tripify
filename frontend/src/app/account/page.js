import UserAccountPanel from "@/components/UserAccountPanel/UserAccountPanel";

export const metadata = {
    title: "Panel użytkownika | Tripify",
};

export default function AccountPage() {
    return (
        <main className="flex min-h-[calc(100vh-73px)] w-full justify-center px-4 py-10">
            <UserAccountPanel />
        </main>
    );
}
