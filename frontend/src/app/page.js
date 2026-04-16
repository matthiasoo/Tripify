import Image from "next/image";

export default function Home() {
    const num = 10;

    return (
        <div className="flex justify-center items-center font-bold flex-col">
            <h1 className="text-6xl mb-5">Tripify</h1>
            <span className="font-mono font-light">Travels made simple.</span>
        </div>
    );
}
